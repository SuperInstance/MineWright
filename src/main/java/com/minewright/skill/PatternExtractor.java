package com.minewright.skill;

import com.minewright.testutil.TestLogger;
import org.slf4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Identifies recurring patterns in execution sequences for skill extraction.
 *
 * <p><b>Purpose:</b></p>
 * <p>PatternExtractor analyzes stored ExecutionSequence objects to find
 * recurring action patterns. These patterns are the foundation for automatic
 * skill generation in the Voyager architecture.</p>
 *
 * <p><b>Pattern Detection Algorithm:</b></p>
 * <ol>
 *   <li><b>Normalization:</b> Convert sequences to normalized signatures</li>
 *   <li><b>Clustering:</b> Group similar sequences by signature</li>
 *   <li><b>Frequency Analysis:</b> Count occurrences of each pattern</li>
 *   <li><b>Parameterization:</b> Extract variable parts for templating</li>
 *   <li><b>Success Rate:</b> Calculate reliability of each pattern</li>
 * </ol>
 *
 * <p><b>Example Pattern:</b></p>
 * <pre>
 * Signature: "pathfind->mine:blockType=stone->place:blockType=torch"
 * Frequency: 15 occurrences
 * Success Rate: 93.3%
 * Parameters: blockType (variable), depth (variable)
 * </pre>
 *
 * <p><b>Thread Safety:</b></p>
 * <p>This class is thread-safe. Pattern extraction is a read-only operation
 * on the provided sequences.</p>
 *
 * @see ExecutionSequence
 * @see ActionRecord
 * @see SkillAutoGenerator
 * @since 1.0.0
 */
public class PatternExtractor {
    private static final Logger LOGGER = TestLogger.getLogger(PatternExtractor.class);

    /**
     * Minimum frequency for a pattern to be considered for skill generation.
     * Patterns appearing fewer times are considered coincidental.
     */
    private static final int MIN_FREQUENCY = 3;

    /**
     * Minimum success rate for a pattern to be viable as a skill.
     * Patterns with lower success rates need refinement before use.
     */
    private static final double MIN_SUCCESS_RATE = 0.7;

    /**
     * Extracts all patterns from the given sequences.
     *
     * @param sequences List of execution sequences to analyze
     * @return List of discovered patterns, sorted by frequency
     */
    public List<Pattern> extractPatterns(List<ExecutionSequence> sequences) {
        if (sequences == null || sequences.isEmpty()) {
            LOGGER.debug("No sequences provided for pattern extraction");
            return List.of();
        }

        LOGGER.info("Extracting patterns from {} sequences", sequences.size());

        // Group sequences by normalized signature
        Map<String, List<ExecutionSequence>> signatureGroups = groupBySignature(sequences);

        // Create patterns from groups
        List<Pattern> patterns = new ArrayList<>();
        for (Map.Entry<String, List<ExecutionSequence>> entry : signatureGroups.entrySet()) {
            String signature = entry.getKey();
            List<ExecutionSequence> group = entry.getValue();

            Pattern pattern = createPattern(signature, group);
            if (pattern != null && meetsThresholds(pattern)) {
                patterns.add(pattern);
            }
        }

        // Sort by frequency (most common first)
        patterns.sort((a, b) -> Integer.compare(b.getFrequency(), a.getFrequency()));

        LOGGER.info("Extracted {} viable patterns from {} sequences",
            patterns.size(), sequences.size());

        return patterns;
    }

    /**
     * Groups sequences by their normalized signatures.
     *
     * @param sequences List of sequences to group
     * @return Map of signature to sequences
     */
    private Map<String, List<ExecutionSequence>> groupBySignature(List<ExecutionSequence> sequences) {
        Map<String, List<ExecutionSequence>> groups = new HashMap<>();

        for (ExecutionSequence sequence : sequences) {
            String signature = sequence.getSignature();
            groups.computeIfAbsent(signature, k -> new ArrayList<>()).add(sequence);
        }

        LOGGER.debug("Found {} unique signatures across {} sequences",
            groups.size(), sequences.size());

        return groups;
    }

    /**
     * Creates a pattern from a group of similar sequences.
     *
     * @param signature The normalized signature
     * @param sequences Sequences matching this signature
     * @return New Pattern object, or null if insufficient data
     */
    private Pattern createPattern(String signature, List<ExecutionSequence> sequences) {
        if (sequences.isEmpty()) {
            return null;
        }

        int frequency = sequences.size();
        int successfulCount = (int) sequences.stream()
            .filter(ExecutionSequence::isSuccessful)
            .count();

        double successRate = (double) successfulCount / frequency;

        // Extract action sequence from first sequence
        List<String> actionSequence = sequences.get(0).getActions().stream()
            .map(ActionRecord::getActionType)
            .collect(Collectors.toList());

        // Extract parameters
        Set<String> parameters = extractParameters(sequences);

        // Generate name from most common goal
        String name = generateName(sequences);

        // Calculate average execution time
        double avgExecutionTime = sequences.stream()
            .mapToLong(ExecutionSequence::getTotalExecutionTime)
            .average()
            .orElse(0);

        return new Pattern(
            signature,
            name,
            actionSequence,
            parameters,
            frequency,
            successRate,
            avgExecutionTime,
            successfulCount,
            frequency - successfulCount
        );
    }

    /**
     * Extracts variable parameters from sequences.
     *
     * @param sequences List of sequences to analyze
     * @return Set of parameter names that vary across sequences
     */
    private Set<String> extractParameters(List<ExecutionSequence> sequences) {
        Set<String> parameters = new HashSet<>();

        if (sequences.isEmpty()) {
            return parameters;
        }

        // Collect all parameters from all sequences
        for (ExecutionSequence sequence : sequences) {
            for (ActionRecord action : sequence.getActions()) {
                action.getParameters().keySet().stream()
                    .filter(this::isVariableParameter)
                    .forEach(parameters::add);
            }
        }

        return parameters;
    }

    /**
     * Checks if a parameter is variable (should be templated).
     * Variable parameters are those that change between executions.
     *
     * @param paramName Parameter name to check
     * @return true if parameter should be templated
     */
    private boolean isVariableParameter(String paramName) {
        String lower = paramName.toLowerCase();
        // Coordinates, counts, and amounts are typically variable
        return lower.contains("x") || lower.contains("y") || lower.contains("z") ||
            lower.contains("count") || lower.contains("amount") ||
            lower.contains("quantity") || lower.contains("radius") ||
            lower.contains("depth") || lower.contains("length") ||
            lower.contains("width") || lower.contains("height") ||
            lower.contains("direction") || lower.contains("target");
    }

    /**
     * Generates a name for the pattern from common goals.
     *
     * @param sequences List of sequences
     * @return Generated pattern name
     */
    private String generateName(List<ExecutionSequence> sequences) {
        // Find most common goal words
        Map<String, Integer> wordFreq = new HashMap<>();

        for (ExecutionSequence seq : sequences) {
            String[] words = seq.getGoal().toLowerCase().split("\\s+");
            for (String word : words) {
                if (word.length() > 3) { // Skip short words
                    wordFreq.merge(word, 1, Integer::sum);
                }
            }
        }

        // Get top word
        String topWord = wordFreq.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("pattern");

        // Capitalize first letter
        return topWord.substring(0, 1).toUpperCase() + topWord.substring(1);
    }

    /**
     * Checks if a pattern meets minimum thresholds for skill generation.
     *
     * @param pattern The pattern to check
     * @return true if pattern is viable
     */
    private boolean meetsThresholds(Pattern pattern) {
        if (pattern.getFrequency() < MIN_FREQUENCY) {
            LOGGER.debug("Pattern '{}' below frequency threshold: {} < {}",
                pattern.getName(), pattern.getFrequency(), MIN_FREQUENCY);
            return false;
        }

        if (pattern.getSuccessRate() < MIN_SUCCESS_RATE) {
            LOGGER.debug("Pattern '{}' below success rate threshold: {:.2f} < {:.2f}",
                pattern.getName(), pattern.getSuccessRate(), MIN_SUCCESS_RATE);
            return false;
        }

        return true;
    }

    /**
     * Represents a discovered pattern in execution sequences.
     */
    public static class Pattern {
        private final String signature;
        private final String name;
        private final List<String> actionSequence;
        private final Set<String> parameters;
        private final int frequency;
        private final double successRate;
        private final double averageExecutionTime;
        private final int successCount;
        private final int failureCount;

        public Pattern(String signature, String name, List<String> actionSequence,
                      Set<String> parameters, int frequency, double successRate,
                      double averageExecutionTime, int successCount, int failureCount) {
            this.signature = signature;
            this.name = name;
            this.actionSequence = List.copyOf(actionSequence);
            this.parameters = Set.copyOf(parameters);
            this.frequency = frequency;
            this.successRate = successRate;
            this.averageExecutionTime = averageExecutionTime;
            this.successCount = successCount;
            this.failureCount = failureCount;
        }

        public String getSignature() {
            return signature;
        }

        public String getName() {
            return name;
        }

        public List<String> getActionSequence() {
            return actionSequence;
        }

        public Set<String> getParameters() {
            return parameters;
        }

        public int getFrequency() {
            return frequency;
        }

        public double getSuccessRate() {
            return successRate;
        }

        public double getAverageExecutionTime() {
            return averageExecutionTime;
        }

        public int getSuccessCount() {
            return successCount;
        }

        public int getFailureCount() {
            return failureCount;
        }

        /**
         * Gets a confidence score for this pattern (0.0 to 1.0).
         * Combines frequency and success rate.
         *
         * @return Confidence score
         */
        public double getConfidence() {
            // Weight success rate more heavily than frequency
            return (successRate * 0.7) + (Math.min(frequency / 10.0, 1.0) * 0.3);
        }

        /**
         * Checks if this pattern is high-confidence (suitable for auto-generation).
         *
         * @return true if confidence >= 0.8
         */
        public boolean isHighConfidence() {
            return getConfidence() >= 0.8;
        }

        @Override
        public String toString() {
            return String.format("Pattern[name=%s, frequency=%d, successRate=%.2f%%, confidence=%.2f]",
                name, frequency, successRate * 100, getConfidence());
        }
    }
}
