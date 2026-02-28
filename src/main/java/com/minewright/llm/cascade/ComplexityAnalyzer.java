package com.minewright.llm.cascade;
import com.minewright.testutil.TestLogger;
import org.slf4j.Logger;

import com.minewright.entity.ForemanEntity;
import com.minewright.memory.WorldKnowledge;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Analyzes commands to determine task complexity for intelligent LLM routing.
 *
 * <p><b>Analysis Factors:</b></p>
 * <ul>
 *   <li><b>Pattern Matching:</b> Known task types identified via regex</li>
 *   <li><b>Command Length:</b> Longer commands typically indicate complexity</li>
 *   <li><b>Keyword Analysis:</b> Presence of complexity-indicating keywords</li>
 *   <li><b>Context Complexity:</b> Number of entities, blocks, coordinates involved</li>
 *   <li><b>Historical Data:</b> Similar commands from past executions</li>
 * </ul>
 *
 * <p><b>Design Pattern:</b> Chain of responsibility for multiple analysis strategies</p>
 * <p><b>Thread Safety:</b> All operations are thread-safe via concurrent collections</p>
 *
 * @since 1.6.0
 */
public class ComplexityAnalyzer {
    /**
     * Logger for this class.
     * Uses TestLogger to avoid triggering MineWrightMod initialization during tests.
     */
    private static final Logger LOGGER = TestLogger.getLogger(ComplexityAnalyzer.class);


    // ------------------------------------------------------------------------
    // Pattern Definitions
    // ------------------------------------------------------------------------

    /**
     * Patterns for TRIVIAL tasks - single action, well-known.
     */
    private static final Pattern[] TRIVIAL_PATTERNS = {
        Pattern.compile("^\\s*stop\\s*$", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^\\s*wait\\s*$", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^\\s*follow\\s+me\\s*$", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^\\s*stay\\s+here\\s*$", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^\\s*come\\s+here\\s*$", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^\\s*status\\s*$", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^\\s*report\\s*$", Pattern.CASE_INSENSITIVE)
    };

    /**
     * Patterns for SIMPLE tasks - 1-2 actions, straightforward.
     */
    private static final Pattern[] SIMPLE_PATTERNS = {
        Pattern.compile("^mine\\s+\\d+\\s+\\w+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^gather\\s+\\d+\\s+\\w+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^craft\\s+\\d+\\s+\\w+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^place\\s+\\w+\\s+(at|near)?\\s*$", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^attack\\s+\\w+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^go\\s+to\\s+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^walk\\s+to\\s+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^pathfind\\s+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^look\\s+at\\s+", Pattern.CASE_INSENSITIVE)
    };

    /**
     * Patterns for MODERATE tasks - 3-5 actions, some reasoning.
     */
    private static final Pattern[] MODERATE_PATTERNS = {
        Pattern.compile("^build\\s+(a\\s+)?\\w+\\s+(house|shelter|structure)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^create\\s+(a\\s+)?\\w+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^construct\\s+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^farm\\s+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^setup\\s+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^gather\\s+resources\\s+for", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^clear\\s+(the\\s+)?area", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^organize\\s+", Pattern.CASE_INSENSITIVE)
    };

    /**
     * Patterns for COMPLEX tasks - multiple actions, coordination.
     */
    private static final Pattern[] COMPLEX_PATTERNS = {
        Pattern.compile("^coordinate\\s+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^work\\s+together\\s+to", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^collaborate\\s+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^team\\s+up\\s+to", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^coordinate\\s+the\\s+crew", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^everyone\\s+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^all\\s+agents\\s+", Pattern.CASE_INSENSITIVE)
    };

    /**
     * Keywords that indicate complexity.
     */
    private static final String[] COMPLEXITY_KEYWORDS = {
        "coordinate", "collaborate", "together", "team", "strategy", "plan",
        "optimize", "efficient", "automated", "complex", "advanced", "multiple",
        "simultaneous", "parallel", "synchronize"
    };

    // ------------------------------------------------------------------------
    // Historical Tracking
    // ------------------------------------------------------------------------

    /**
     * Command frequency tracking for novelty detection.
     * Maps command signature -> execution count.
     */
    private final Map<String, Integer> commandHistory = new ConcurrentHashMap<>();

    /**
     * Maximum history size to prevent unbounded growth.
     */
    private static final int MAX_HISTORY_SIZE = 1000;

    // ------------------------------------------------------------------------
    // Analysis Methods
    // ------------------------------------------------------------------------

    /**
     * Analyzes a command to determine its complexity level.
     *
     * <p>Uses multiple analysis strategies in sequence:</p>
     * <ol>
     *   <li>Pattern matching against known task types</li>
     *   <li>Command length and structure analysis</li>
     *   <li>Keyword presence checking</li>
     *   <li>Historical frequency checking (novelty detection)</li>
     *   <li>Context complexity assessment</li>
     * </ol>
     *
     * @param command The user command to analyze
     * @param foreman The crew member entity (for context)
     * @param worldKnowledge World state information
     * @return Determined task complexity
     */
    public TaskComplexity analyze(String command, ForemanEntity foreman, WorldKnowledge worldKnowledge) {
        if (command == null || command.trim().isEmpty()) {
            return TaskComplexity.TRIVIAL;
        }

        String normalizedCommand = command.trim().toLowerCase();

        // Step 1: Pattern matching (highest confidence)
        TaskComplexity patternResult = analyzeByPattern(normalizedCommand);
        if (patternResult != null) {
            return adjustForHistory(command, patternResult);
        }

        // Step 2: Length and structure analysis
        TaskComplexity lengthResult = analyzeByLength(command);
        if (lengthResult != null) {
            return adjustForHistory(command, lengthResult);
        }

        // Step 3: Keyword analysis
        TaskComplexity keywordResult = analyzeByKeywords(normalizedCommand);
        if (keywordResult != null) {
            return adjustForHistory(command, keywordResult);
        }

        // Step 4: Context analysis
        TaskComplexity contextResult = analyzeByContext(command, foreman, worldKnowledge);
        if (contextResult != null) {
            return adjustForHistory(command, contextResult);
        }

        // Default to MODERATE if no strong signals
        return adjustForHistory(command, TaskComplexity.MODERATE);
    }

    /**
     * Analyzes command by matching against known patterns.
     *
     * @param normalizedCommand Normalized (lowercase) command
     * @return Complexity level if pattern matched, null otherwise
     */
    private TaskComplexity analyzeByPattern(String normalizedCommand) {
        // Check TRIVIAL patterns first (highest priority)
        for (Pattern pattern : TRIVIAL_PATTERNS) {
            if (pattern.matcher(normalizedCommand).matches()) {
                return TaskComplexity.TRIVIAL;
            }
        }

        // Check SIMPLE patterns
        for (Pattern pattern : SIMPLE_PATTERNS) {
            if (pattern.matcher(normalizedCommand).find()) {
                return TaskComplexity.SIMPLE;
            }
        }

        // Check MODERATE patterns
        for (Pattern pattern : MODERATE_PATTERNS) {
            if (pattern.matcher(normalizedCommand).find()) {
                return TaskComplexity.MODERATE;
            }
        }

        // Check COMPLEX patterns
        for (Pattern pattern : COMPLEX_PATTERNS) {
            if (pattern.matcher(normalizedCommand).find()) {
                return TaskComplexity.COMPLEX;
            }
        }

        return null;
    }

    /**
     * Analyzes command by length and structure.
     *
     * @param command The original command
     * @return Complexity level based on length analysis
     */
    private TaskComplexity analyzeByLength(String command) {
        int wordCount = command.split("\\s+").length;
        int charCount = command.length();

        // Count sentences (rough heuristic)
        int sentenceCount = command.split("[.!?]+").length;
        int conjunctionCount = countWords(command, new String[]{"and", "then", "after", "while"});

        // Very short commands are likely TRIVIAL
        if (wordCount <= 3 && charCount < 30) {
            return TaskComplexity.TRIVIAL;
        }

        // Short single-sentence commands are SIMPLE
        if (wordCount <= 10 && sentenceCount == 1 && conjunctionCount == 0) {
            return TaskComplexity.SIMPLE;
        }

        // Multi-part commands are MODERATE
        if (wordCount <= 25 && sentenceCount <= 2 && conjunctionCount <= 2) {
            return TaskComplexity.MODERATE;
        }

        // Long complex commands are COMPLEX
        if (wordCount > 25 || sentenceCount > 2 || conjunctionCount > 2) {
            return TaskComplexity.COMPLEX;
        }

        return null;
    }

    /**
     * Analyzes command by keyword presence.
     *
     * @param normalizedCommand Normalized (lowercase) command
     * @return Complexity level based on keywords
     */
    private TaskComplexity analyzeByKeywords(String normalizedCommand) {
        int complexityKeywordCount = 0;

        for (String keyword : COMPLEXITY_KEYWORDS) {
            if (normalizedCommand.contains(keyword)) {
                complexityKeywordCount++;
            }
        }

        // Multiple complexity keywords indicate COMPLEX task
        if (complexityKeywordCount >= 2) {
            return TaskComplexity.COMPLEX;
        }

        // Single complexity keyword indicates MODERATE task
        if (complexityKeywordCount == 1) {
            return TaskComplexity.MODERATE;
        }

        return null;
    }

    /**
     * Analyzes command based on world context.
     *
     * @param command The command
     * @param foreman The crew member
     * @param worldKnowledge World state
     * @return Complexity based on context factors
     */
    private TaskComplexity analyzeByContext(String command, ForemanEntity foreman,
                                             WorldKnowledge worldKnowledge) {
        if (worldKnowledge == null) {
            return null;
        }

        // Check for multi-agent coordination
        boolean involvesMultipleAgents = command.toLowerCase().matches(".*(everyone|all|team|coordinate).*");

        // Check for complex block interactions
        int blockTypeCount = countUniqueBlocks(command);

        // Check for coordinate precision
        boolean hasPreciseCoordinates = command.matches(".*\\b\\d+,\\s*\\d+,\\s*\\d+b.*");

        // Complex context -> COMPLEX
        if (involvesMultipleAgents || blockTypeCount > 5 || hasPreciseCoordinates) {
            return TaskComplexity.COMPLEX;
        }

        // Moderate context -> MODERATE
        if (blockTypeCount > 2) {
            return TaskComplexity.MODERATE;
        }

        return null;
    }

    /**
     * Adjusts complexity based on historical execution frequency.
     * <p>Frequently seen commands may be downgraded (cache hit likely).
     * Never-seen commands are marked as NOVEL.</p>
     *
     * @param command The command signature
     * @param initialComplexity Initial complexity from other analysis
     * @return Adjusted complexity level
     */
    private TaskComplexity adjustForHistory(String command, TaskComplexity initialComplexity) {
        String signature = generateCommandSignature(command);
        int executionCount = commandHistory.getOrDefault(signature, 0);

        // Track this command
        trackCommand(signature);

        // Never seen before -> NOVEL
        if (executionCount == 0) {
            // Only downgrade to NOVEL for non-trivial tasks
            return initialComplexity == TaskComplexity.TRIVIAL
                ? TaskComplexity.TRIVIAL
                : TaskComplexity.NOVEL;
        }

        // Frequently seen -> may downgrade for cache optimization
        if (executionCount >= 5) {
            // TRIVIAL tasks are already cached
            if (initialComplexity == TaskComplexity.SIMPLE) {
                return TaskComplexity.TRIVIAL;
            }
            // MODERATE tasks seen frequently may be SIMPLE
            if (initialComplexity == TaskComplexity.MODERATE && executionCount >= 10) {
                return TaskComplexity.SIMPLE;
            }
        }

        return initialComplexity;
    }

    // ------------------------------------------------------------------------
    // Utility Methods
    // ------------------------------------------------------------------------

    /**
     * Counts occurrences of specific words in a command.
     *
     * @param command The command to search
     * @param words Words to count
     * @return Total count of word occurrences
     */
    private int countWords(String command, String[] words) {
        String normalized = command.toLowerCase();
        int count = 0;
        for (String word : words) {
            if (normalized.contains("\\b" + word + "\\b")) {
                count++;
            }
        }
        return count;
    }

    /**
     * Estimates number of unique block types mentioned in command.
     *
     * @param command The command
     * @return Estimated unique block count
     */
    private int countUniqueBlocks(String command) {
        // Simple heuristic: count Minecraft block-related words
        String[] blockIndicators = {
            "stone", "wood", "dirt", "cobblestone", "plank", "ore", "ingot",
            "block", "slab", "stair", "fence", "door", "glass", "brick"
        };

        int count = 0;
        String normalized = command.toLowerCase();
        for (String indicator : blockIndicators) {
            if (normalized.contains(indicator)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Generates a signature for command history tracking.
     * <p>Normalizes by removing quantities and specific values.</p>
     *
     * @param command The command
     * @return Command signature for history matching
     */
    private String generateCommandSignature(String command) {
        // Remove numbers
        String normalized = command.replaceAll("\\d+", "N");
        // Normalize whitespace
        normalized = normalized.trim().toLowerCase().replaceAll("\\s+", " ");
        return normalized;
    }

    /**
     * Records a command execution for history tracking.
     *
     * @param signature Command signature
     */
    private void trackCommand(String signature) {
        commandHistory.merge(signature, 1, Integer::sum);

        // Prevent unbounded growth
        if (commandHistory.size() > MAX_HISTORY_SIZE) {
            // Remove oldest entries (simple FIFO eviction)
            commandHistory.keySet().stream()
                .findFirst()
                .ifPresent(commandHistory::remove);
        }
    }

    /**
     * Returns statistics about command history.
     *
     * @return Total unique commands tracked
     */
    public int getHistorySize() {
        return commandHistory.size();
    }

    /**
     * Returns how many times a command pattern has been seen.
     *
     * @param command The command to check
     * @return Execution count, or 0 if never seen
     */
    public int getExecutionCount(String command) {
        String signature = generateCommandSignature(command);
        return commandHistory.getOrDefault(signature, 0);
    }

    /**
     * Clears command history (for testing or reset).
     */
    public void clearHistory() {
        LOGGER.info("Clearing complexity analyzer history ({} entries)", commandHistory.size());
        commandHistory.clear();
    }
}
