package com.minewright.llm;

import com.minewright.llm.cascade.LLMTier;
import com.minewright.llm.cascade.TaskComplexity;
import com.minewright.testutil.TestLogger;
import org.slf4j.Logger;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Fast prompt complexity classifier for intelligent LLM routing.
 *
 * <p><b>Design Philosophy:</b> Rule-based classification with optional local LLM fallback.
 * The rule-based approach ensures < 10ms latency for 95% of cases, with the local LLM
 * (SmolLM2-360M) available for ambiguous cases that need deeper understanding.</p>
 *
 * <p><b>Performance:</b></p>
 * <ul>
 *   <li>Rule-based path: < 10ms (99% of cases)</li>
 *   <li>Local LLM path: < 100ms (1% ambiguous cases)</li>
 *   <li>Never calls cloud LLMs for classification</li>
 * </ul>
 *
 * <p><b>Categories:</b></p>
 * <ul>
 *   <li><b>TRIVIAL:</b> Single-word commands, cache hits (0-3 words)</li>
 *   <li><b>SIMPLE:</b> Single action, straightforward (4-10 words)</li>
 *   <li><b>MODERATE:</b> Multi-step, some reasoning (11-25 words)</li>
 *   <li><b>COMPLEX:</b> Coordinated, multi-agent, creative (25+ words)</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b> Immutable and thread-safe</p>
 *
 * @since 1.7.0
 */
public class ComplexityClassifier {

    private static final Logger LOGGER = TestLogger.getLogger(ComplexityClassifier.class);

    // ------------------------------------------------------------------------
    // System Prompts for Local LLM Classification
    // ------------------------------------------------------------------------

    /**
     * System prompt for SmolLM2-360M classification.
     * Optimized for small models - clear, concise instructions with examples.
     */
    public static final String CLASSIFICATION_SYSTEM_PROMPT = """
        You are a prompt complexity classifier. Determine which LLM tier should handle this Minecraft command.

        CATEGORIES:
        - TRIVIAL: Single action, well-known (e.g., "stop", "follow me", "wait")
        - SIMPLE: 1-2 straightforward actions (e.g., "mine 10 iron", "go to 100 64 100")
        - MODERATE: 3-5 actions, some reasoning (e.g., "build a small house", "gather resources for tools")
        - COMPLEX: Multi-agent, creative, or ambiguous (e.g., "coordinate crew to build castle", "design efficient farm")

        MINECRAFT-ACTION KEYWORDS:
        - TRIVIAL: stop, wait, stay, follow, come, status, report
        - SIMPLE: mine, gather, craft, place, attack, go, walk, look
        - MODERATE: build, create, construct, farm, setup, clear
        - COMPLEX: coordinate, collaborate, team, strategy, optimize, design

        RESPOND WITH JSON:
        {"complexity":"TRIVIAL|SIMPLE|MODERATE|COMPLEX","tier":"CACHE|FAST|BALANCED|SMART","reasoning":"brief explanation"}

        EXAMPLES:
        "stop" -> {"complexity":"TRIVIAL","tier":"CACHE","reasoning":"Single known command"}
        "mine 10 iron" -> {"complexity":"SIMPLE","tier":"FAST","reasoning":"Single action with quantity"}
        "build a house" -> {"complexity":"MODERATE","tier":"BALANCED","reasoning":"Multi-step construction"}
        "coordinate crew to build castle" -> {"complexity":"COMPLEX","tier":"SMART","reasoning":"Multi-agent coordination"}
        """;

    /**
     * Simplified system prompt for faster classification on smaller models.
     */
    public static final String CLASSIFICATION_SYSTEM_PROMPT_FAST = """
        Classify this Minecraft command: TRIVIAL, SIMPLE, MODERATE, or COMPLEX.

        TRIVIAL: stop, wait, follow me, status
        SIMPLE: mine X, craft X, go to X, attack
        MODERATE: build X, farm X, gather resources for X
        COMPLEX: coordinate team, design, optimize, complex strategy

        Respond only with the category name.
        """;

    // ------------------------------------------------------------------------
    // Minecraft-Specific Patterns and Keywords
    // ------------------------------------------------------------------------

    // Trivial patterns - single action, well-known
    private static final Pattern[] TRIVIAL_PATTERNS = {
        Pattern.compile("^\\s*stop\\s*$", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^\\s*wait\\s*$", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^\\s*pause\\s*$", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^\\s*follow\\s+me\\s*$", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^\\s*stay\\s+here\\s*$", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^\\s*come\\s+here\\s*$", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^\\s*status\\s*$", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^\\s*report\\s*$", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^\\s*help\\s*$", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^\\s*what\\s+can\\s+you\\s+do\\s*$", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^\\s*hello\\s*$", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^\\s*hi\\s*$", Pattern.CASE_INSENSITIVE)
    };

    // Simple patterns - single action with parameters
    private static final Pattern[] SIMPLE_PATTERNS = {
        Pattern.compile("^mine\\s+(\\d+\\s+)?\\w+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^gather\\s+(\\d+\\s+)?\\w+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^collect\\s+(\\d+\\s+)?\\w+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^craft\\s+(\\d+\\s+)?\\w+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^place\\s+\\w+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^put\\s+\\w+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^attack\\s+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^kill\\s+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^fight\\s+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^go\\s+to\\s+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^walk\\s+to\\s+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^move\\s+to\\s+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^pathfind\\s+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^look\\s+at\\s+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^face\\s+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^equip\\s+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^use\\s+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^eat\\s+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^drop\\s+", Pattern.CASE_INSENSITIVE)
    };

    // Moderate patterns - multi-step tasks
    private static final Pattern[] MODERATE_PATTERNS = {
        Pattern.compile("^build\\s+(a\\s+)?\\w+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^create\\s+(a\\s+)?\\w+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^construct\\s+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^make\\s+(a\\s+)?\\w+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^farm\\s+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^setup\\s+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^set\\s+up\\s+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^gather\\s+resources\\s+for", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^collect\\s+materials\\s+for", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^clear\\s+(the\\s+)?area", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^clean\\s+(the\\s+)?area", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^organize\\s+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^sort\\s+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^explore\\s+", Pattern.CASE_INSENSITIVE)
    };

    // Complex patterns - multi-agent, creative, or ambiguous
    private static final Pattern[] COMPLEX_PATTERNS = {
        Pattern.compile("^coordinate\\s+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^work\\s+together\\s+to", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^collaborate\\s+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^team\\s+up\\s+to", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^everyone\\s+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^all\\s+agents\\s+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^design\\s+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^optimize\\s+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^strategy\\s+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^plan\\s+(a\\s+)?strategy", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^figure\\s+out\\s+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^solve\\s+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^debug\\s+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("what'?s\\s+the\\s+best", Pattern.CASE_INSENSITIVE),
        Pattern.compile("what\\s+is\\s+the\\s+best", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^how\\s+(should|can|do|to)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^why\\s+", Pattern.CASE_INSENSITIVE)
    };

    // Keywords by complexity level
    private static final Set<String> TRIVIAL_KEYWORDS = Set.of(
        "stop", "wait", "pause", "stay", "hello", "hi", "hey", "status", "report", "help"
    );

    private static final Set<String> SIMPLE_KEYWORDS = Set.of(
        "mine", "gather", "collect", "craft", "place", "put", "attack", "kill", "fight",
        "go", "walk", "move", "run", "travel", "pathfind", "look", "face", "watch",
        "equip", "use", "eat", "drop", "throw", "hold", "wear"
    );

    private static final Set<String> MODERATE_KEYWORDS = Set.of(
        "build", "create", "construct", "make", "farm", "setup", "set up", "clear", "clean",
        "organize", "sort", "arrange", "explore", "find", "search", "hunt", "chop"
    );

    private static final Set<String> COMPLEX_KEYWORDS = Set.of(
        "coordinate", "collaborate", "together", "team", "everyone", "design", "optimize",
        "strategy", "plan", "figure out", "solve", "debug", "best way", "efficient",
        "automate", "automated", "advanced", "complex", "multiple", "simultaneous"
    );

    // Vision-related keywords (may require multimodal model)
    private static final Set<String> VISION_KEYWORDS = Set.of(
        "screenshot", "picture", "image", "photo", "look at this", "see this",
        "what do you see", "identify", "recognize", "analyze this"
    );

    // ------------------------------------------------------------------------
    // Classification Thresholds
    // ------------------------------------------------------------------------

    private static final int TRIVIAL_MAX_WORDS = 3;
    private static final int TRIVIAL_MAX_CHARS = 25;
    private static final int SIMPLE_MAX_WORDS = 10;
    private static final int MODERATE_MAX_WORDS = 25;
    private static final int SIMPLE_MAX_SENTENCES = 1;
    private static final int MODERATE_MAX_SENTENCES = 2;

    // ------------------------------------------------------------------------
    // Local LLM Client (optional)
    // ------------------------------------------------------------------------

    private final LocalLLMClient localLLM;
    private final boolean useLocalLLM;

    /**
     * Creates a classifier with rule-based classification only (fastest).
     */
    public ComplexityClassifier() {
        this(null);
    }

    /**
     * Creates a classifier with optional local LLM for ambiguous cases.
     *
     * @param localLLM Local LLM client (can be null for rule-only mode)
     */
    public ComplexityClassifier(LocalLLMClient localLLM) {
        this.localLLM = localLLM;
        this.useLocalLLM = localLLM != null && localLLM.isAvailable();
        if (useLocalLLM) {
            LOGGER.info("[ComplexityClassifier] Initialized with local LLM support at {}",
                localLLM.getServerUrl());
        } else {
            LOGGER.info("[ComplexityClassifier] Initialized in rule-only mode (fastest)");
        }
    }

    // ------------------------------------------------------------------------
    // Main Classification Methods
    // ------------------------------------------------------------------------

    /**
     * Classifies a command's complexity using rule-based analysis.
     * <p>This is the fast path - typically completes in < 10ms.</p>
     *
     * @param command The user command to classify
     * @return Classification result with complexity, tier, and reasoning
     */
    public ClassificationResult classify(String command) {
        return classify(command, false);
    }

    /**
     * Classifies a command's complexity with optional local LLM fallback.
     *
     * @param command The user command to classify
     * @param allowLocalLLM If true, use local LLM for ambiguous cases
     * @return Classification result with complexity, tier, and reasoning
     */
    public ClassificationResult classify(String command, boolean allowLocalLLM) {
        long startTime = System.nanoTime();

        if (command == null || command.trim().isEmpty()) {
            return result(TaskComplexity.TRIVIAL, LLMTier.CACHE,
                "Empty command", startTime);
        }

        // Try rule-based classification first (fast path)
        ClassificationResult ruleResult = classifyByRules(command);
        if (ruleResult.confidence() >= 0.8) {
            // High confidence in rule-based result
            return result(ruleResult.complexity(), ruleResult.tier(),
                ruleResult.reasoning(), startTime);
        }

        // Low confidence - try local LLM if available and allowed
        if (allowLocalLLM && useLocalLLM) {
            ClassificationResult llmResult = classifyByLocalLLM(command);
            if (llmResult != null) {
                return result(llmResult.complexity(), llmResult.tier(),
                    llmResult.reasoning() + " (LLM-verified)", startTime);
            }
        }

        // Fall back to rule-based result
        return result(ruleResult.complexity(), ruleResult.tier(),
            ruleResult.reasoning(), startTime);
    }

    /**
     * Fast rule-based classification without any LLM calls.
     *
     * @param command The user command to classify
     * @return Classification result with confidence score
     */
    public ClassificationResult classifyByRules(String command) {
        if (command == null || command.trim().isEmpty()) {
            return new ClassificationResult(TaskComplexity.TRIVIAL, LLMTier.CACHE,
                "Empty command", 1.0);
        }

        String normalized = command.trim().toLowerCase();

        // Step 0: Check for very long commands first (override patterns)
        // This catches edge cases where long commands contain simple patterns
        int wordCount = command.split("\\s+").length;
        if (wordCount > MODERATE_MAX_WORDS) {
            return new ClassificationResult(TaskComplexity.COMPLEX, LLMTier.SMART,
                "Very long command (" + wordCount + " words)", 0.9);
        }

        // Step 1: Pattern matching (high confidence)
        double patternConfidence = 0.95;

        if (matchesAny(TRIVIAL_PATTERNS, normalized)) {
            return new ClassificationResult(TaskComplexity.TRIVIAL, LLMTier.CACHE,
                "Matches trivial pattern", patternConfidence);
        }

        if (matchesAny(SIMPLE_PATTERNS, normalized)) {
            return new ClassificationResult(TaskComplexity.SIMPLE, LLMTier.FAST,
                "Matches simple pattern", patternConfidence);
        }

        if (matchesAny(MODERATE_PATTERNS, normalized)) {
            return new ClassificationResult(TaskComplexity.MODERATE, LLMTier.BALANCED,
                "Matches moderate pattern", patternConfidence);
        }

        if (matchesAny(COMPLEX_PATTERNS, normalized)) {
            return new ClassificationResult(TaskComplexity.COMPLEX, LLMTier.SMART,
                "Matches complex pattern", patternConfidence);
        }

        // Step 2: Length-based analysis (medium confidence)
        double lengthConfidence = 0.7;
        TaskComplexity lengthComplexity = analyzeByLength(command);
        if (lengthComplexity != null) {
            LLMTier tier = tierForComplexity(lengthComplexity);
            return new ClassificationResult(lengthComplexity, tier,
                reasonForLength(lengthComplexity), lengthConfidence);
        }

        // Step 3: Keyword analysis (lower confidence)
        double keywordConfidence = 0.5;
        TaskComplexity keywordComplexity = analyzeByKeywords(normalized);
        LLMTier tier = tierForComplexity(keywordComplexity);
        return new ClassificationResult(keywordComplexity, tier,
            "Based on keyword analysis", keywordConfidence);
    }

    /**
     * Uses the local SmolLM2-360M model for classification.
     * <p>Only called for ambiguous cases - typically < 5% of requests.</p>
     *
     * @param command The user command to classify
     * @return Classification result, or null if local LLM unavailable
     */
    public ClassificationResult classifyByLocalLLM(String command) {
        if (localLLM == null || !localLLM.isAvailable()) {
            return null;
        }

        try {
            String response = localLLM.sendRequest(CLASSIFICATION_SYSTEM_PROMPT_FAST, command);
            if (response == null || response.isEmpty()) {
                return null;
            }

            // Parse the response
            String normalized = response.trim().toUpperCase();

            // Extract complexity from response
            TaskComplexity complexity = null;
            if (normalized.contains("TRIVIAL")) {
                complexity = TaskComplexity.TRIVIAL;
            } else if (normalized.contains("SIMPLE")) {
                complexity = TaskComplexity.SIMPLE;
            } else if (normalized.contains("MODERATE")) {
                complexity = TaskComplexity.MODERATE;
            } else if (normalized.contains("COMPLEX")) {
                complexity = TaskComplexity.COMPLEX;
            }

            if (complexity != null) {
                LLMTier tier = tierForComplexity(complexity);
                return new ClassificationResult(complexity, tier,
                    "Classified by local LLM: " + response.trim(), 0.9);
            }
        } catch (Exception e) {
            LOGGER.warn("[ComplexityClassifier] Local LLM classification failed: {}",
                e.getMessage());
        }

        return null;
    }

    // ------------------------------------------------------------------------
    // Analysis Helper Methods
    // ------------------------------------------------------------------------

    /**
     * Analyzes command complexity based on length and structure.
     *
     * @param command The command to analyze
     * @return Detected complexity, or null if unclear
     */
    private TaskComplexity analyzeByLength(String command) {
        int wordCount = command.split("\\s+").length;
        int charCount = command.length();
        int sentenceCount = command.split("[.!?]+").length;
        int conjunctionCount = countConjunctions(command);

        // Very short commands
        if (wordCount <= TRIVIAL_MAX_WORDS && charCount <= TRIVIAL_MAX_CHARS) {
            return TaskComplexity.TRIVIAL;
        }

        // Long commands with many words or sentences indicate complexity
        if (wordCount > MODERATE_MAX_WORDS || sentenceCount > MODERATE_MAX_SENTENCES || conjunctionCount > 2) {
            return TaskComplexity.COMPLEX;
        }

        // Multi-part commands with conjunctions
        if (wordCount > SIMPLE_MAX_WORDS || conjunctionCount > 0) {
            return TaskComplexity.MODERATE;
        }

        // Short single-sentence commands
        return TaskComplexity.SIMPLE;
    }

    /**
     * Analyzes command complexity based on keyword presence.
     *
     * @param normalized Lowercase command
     * @return Detected complexity
     */
    private TaskComplexity analyzeByKeywords(String normalized) {
        int trivialCount = countKeywords(normalized, TRIVIAL_KEYWORDS);
        int simpleCount = countKeywords(normalized, SIMPLE_KEYWORDS);
        int moderateCount = countKeywords(normalized, MODERATE_KEYWORDS);
        int complexCount = countKeywords(normalized, COMPLEX_KEYWORDS);

        // Complex keywords have highest weight
        if (complexCount >= 2) {
            return TaskComplexity.COMPLEX;
        }
        if (complexCount == 1) {
            return TaskComplexity.MODERATE;
        }

        // Moderate keywords
        if (moderateCount >= 2) {
            return TaskComplexity.MODERATE;
        }
        if (moderateCount == 1) {
            return TaskComplexity.SIMPLE;
        }

        // Simple keywords
        if (simpleCount >= 1) {
            return TaskComplexity.SIMPLE;
        }

        // Trivial keywords
        if (trivialCount >= 1) {
            return TaskComplexity.TRIVIAL;
        }

        // Default to moderate for unknown
        return TaskComplexity.MODERATE;
    }

    /**
     * Checks if the command requires vision capabilities.
     *
     * @param normalized Lowercase command
     * @return true if vision is needed
     */
    public boolean requiresVision(String normalized) {
        return VISION_KEYWORDS.stream().anyMatch(normalized::contains);
    }

    // ------------------------------------------------------------------------
    // Utility Methods
    // ------------------------------------------------------------------------

    /**
     * Checks if a string matches any pattern in an array.
     */
    private boolean matchesAny(Pattern[] patterns, String input) {
        for (Pattern pattern : patterns) {
            if (pattern.matcher(input).matches() || pattern.matcher(input).find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Counts how many keywords from a set appear in the command.
     */
    private int countKeywords(String text, Set<String> keywords) {
        return (int) keywords.stream().filter(text::contains).count();
    }

    /**
     * Counts conjunctions that indicate multi-part commands.
     */
    private int countConjunctions(String command) {
        String[] conjunctions = {"and", "then", "after", "while", "before", "but"};
        String normalized = " " + command.toLowerCase() + " ";
        int count = 0;
        for (String conjunction : conjunctions) {
            if (normalized.contains(" " + conjunction + " ")) {
                count++;
            }
        }
        return count;
    }

    /**
     * Maps complexity to recommended LLM tier.
     */
    private LLMTier tierForComplexity(TaskComplexity complexity) {
        return switch (complexity) {
            case TRIVIAL -> LLMTier.CACHE;
            case SIMPLE -> LLMTier.FAST;
            case MODERATE -> LLMTier.BALANCED;
            case COMPLEX, NOVEL -> LLMTier.SMART;
        };
    }

    /**
     * Generates reasoning for length-based classification.
     */
    private String reasonForLength(TaskComplexity complexity) {
        return switch (complexity) {
            case TRIVIAL -> "Very short command (0-3 words)";
            case SIMPLE -> "Short command with clear action (4-10 words)";
            case MODERATE -> "Multi-step command (11-25 words)";
            case COMPLEX -> "Long command requiring planning (25+ words)";
            case NOVEL -> "Novel task requiring full reasoning";
        };
    }

    /**
     * Creates a timed result wrapper.
     */
    private ClassificationResult result(TaskComplexity complexity, LLMTier tier,
                                         String reasoning, long startTimeNanos) {
        long durationMs = (System.nanoTime() - startTimeNanos) / 1_000_000;
        return new ClassificationResult(complexity, tier,
            reasoning + " (" + durationMs + "ms)", 1.0);
    }

    // ------------------------------------------------------------------------
    // Nested Classes
    // ------------------------------------------------------------------------

    /**
     * Result of complexity classification.
     *
     * @param complexity Detected task complexity
     * @param tier Recommended LLM tier
     * @param reasoning Human-readable explanation
     * @param confidence Confidence score (0.0 to 1.0)
     */
    public record ClassificationResult(
        TaskComplexity complexity,
        LLMTier tier,
        String reasoning,
        double confidence
    ) {
        /**
         * Checks if this result is high-confidence.
         */
        public boolean isHighConfidence() {
            return confidence >= 0.8;
        }

        /**
         * Checks if this result recommends a cached response.
         */
        public boolean recommendsCache() {
            return tier == LLMTier.CACHE;
        }

        /**
         * Checks if this result recommends cloud LLM.
         */
        public boolean requiresCloudLLM() {
            return tier.requiresApiCall();
        }

        @Override
        public String toString() {
            return String.format("Classification[%s -> %s, %.0f%%, %s]",
                complexity, tier, confidence * 100, reasoning);
        }
    }
}
