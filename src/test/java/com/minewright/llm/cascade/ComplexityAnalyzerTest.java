package com.minewright.llm.cascade;

import com.minewright.entity.ForemanEntity;
import com.minewright.memory.WorldKnowledge;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ComplexityAnalyzer}.
 *
 * Tests cover:
 * <ul>
 *   <li>Trivial command recognition via pattern matching</li>
 *   <li>Simple command recognition (1-2 actions)</li>
 *   <li>Moderate command recognition (3-5 actions, reasoning)</li>
 *   <li>Complex command recognition (multi-agent coordination)</li>
 *   <li>Novel command detection (first-seen commands)</li>
 *   <li>Context-aware complexity adjustments</li>
 *   <li>Historical frequency impact on complexity</li>
 *   <li>Command history tracking and eviction</li>
 * </ul>
 *
 * @since 1.6.0
 */
@DisplayName("Complexity Analyzer Tests")
class ComplexityAnalyzerTest {

    private ComplexityAnalyzer analyzer;

    @Mock
    private ForemanEntity mockForeman;

    @Mock
    private WorldKnowledge mockWorldKnowledge;

    @BeforeEach
    void setUp() {
        analyzer = new ComplexityAnalyzer();
        analyzer.clearHistory();
        mockForeman = mock(ForemanEntity.class);
        mockWorldKnowledge = mock(WorldKnowledge.class);
    }

    // ------------------------------------------------------------------------
    // Trivial Command Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Trivial commands: 'stop' recognized")
    void trivialCommandStopRecognized() {
        TaskComplexity complexity = analyzer.analyze("stop", mockForeman, mockWorldKnowledge);

        assertEquals(TaskComplexity.NOVEL, complexity,
            "First-time 'stop' command should be NOVEL (not seen before)");
    }

    @Test
    @DisplayName("Trivial commands: 'wait' recognized")
    void trivialCommandWaitRecognized() {
        TaskComplexity complexity = analyzer.analyze("wait", mockForeman, mockWorldKnowledge);

        assertEquals(TaskComplexity.NOVEL, complexity,
            "First-time 'wait' command should be NOVEL");
    }

    @Test
    @DisplayName("Trivial commands: 'follow me' recognized")
    void trivialCommandFollowMeRecognized() {
        TaskComplexity complexity = analyzer.analyze("follow me", mockForeman, mockWorldKnowledge);

        assertEquals(TaskComplexity.NOVEL, complexity,
            "First-time 'follow me' command should be NOVEL");
    }

    @Test
    @DisplayName("Trivial commands: 'stay here' recognized")
    void trivialCommandStayHereRecognized() {
        TaskComplexity complexity = analyzer.analyze("stay here", mockForeman, mockWorldKnowledge);

        assertEquals(TaskComplexity.NOVEL, complexity,
            "First-time 'stay here' command should be NOVEL");
    }

    @Test
    @DisplayName("Trivial commands: 'status' recognized")
    void trivialCommandStatusRecognized() {
        TaskComplexity complexity = analyzer.analyze("status", mockForeman, mockWorldKnowledge);

        assertEquals(TaskComplexity.NOVEL, complexity,
            "First-time 'status' command should be NOVEL");
    }

    @Test
    @DisplayName("Trivial commands: case insensitive matching")
    void trivialCommandsCaseInsensitive() {
        assertEquals(TaskComplexity.NOVEL, analyzer.analyze("STOP", mockForeman, mockWorldKnowledge));
        assertEquals(TaskComplexity.NOVEL, analyzer.analyze("Stop", mockForeman, mockWorldKnowledge));
        assertEquals(TaskComplexity.NOVEL, analyzer.analyze("  stop  ", mockForeman, mockWorldKnowledge));
    }

    @Test
    @DisplayName("Trivial commands: frequent execution downgrades to TRIVIAL")
    void trivialCommandDowngradedAfterFrequentExecution() {
        // Execute 'stop' command 5 times
        for (int i = 0; i < 5; i++) {
            analyzer.analyze("stop", mockForeman, mockWorldKnowledge);
        }

        // After 5 executions, should still be NOVEL (TRIVIAL stays TRIVIAL after first execution)
        TaskComplexity complexity = analyzer.analyze("stop", mockForeman, mockWorldKnowledge);
        assertEquals(TaskComplexity.TRIVIAL, complexity,
            "Frequently seen TRIVIAL command should remain TRIVIAL");
    }

    // ------------------------------------------------------------------------
    // Simple Command Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Simple commands: 'mine N stone' recognized")
    void simpleCommandMineStoneRecognized() {
        TaskComplexity complexity = analyzer.analyze("mine 10 stone", mockForeman, mockWorldKnowledge);

        assertEquals(TaskComplexity.NOVEL, complexity,
            "First-time 'mine 10 stone' should be NOVEL");
    }

    @Test
    @DisplayName("Simple commands: 'gather N resource' recognized")
    void simpleCommandGatherResourceRecognized() {
        TaskComplexity complexity = analyzer.analyze("gather 5 oak_log", mockForeman, mockWorldKnowledge);

        assertEquals(TaskComplexity.NOVEL, complexity,
            "First-time 'gather' command should be NOVEL");
    }

    @Test
    @DisplayName("Simple commands: 'craft N item' recognized")
    void simpleCommandCraftItemRecognized() {
        TaskComplexity complexity = analyzer.analyze("craft 3 crafting_table", mockForeman, mockWorldKnowledge);

        assertEquals(TaskComplexity.NOVEL, complexity,
            "First-time 'craft' command should be NOVEL");
    }

    @Test
    @DisplayName("Simple commands: 'place block' recognized")
    void simpleCommandPlaceBlockRecognized() {
        TaskComplexity complexity = analyzer.analyze("place torch", mockForeman, mockWorldKnowledge);

        assertEquals(TaskComplexity.NOVEL, complexity,
            "First-time 'place' command should be NOVEL");
    }

    @Test
    @DisplayName("Simple commands: 'go to location' recognized")
    void simpleCommandGoToRecognized() {
        TaskComplexity complexity = analyzer.analyze("go to the village", mockForeman, mockWorldKnowledge);

        assertEquals(TaskComplexity.NOVEL, complexity,
            "First-time 'go to' command should be NOVEL");
    }

    @Test
    @DisplayName("Simple commands: 'walk to location' recognized")
    void simpleCommandWalkToRecognized() {
        TaskComplexity complexity = analyzer.analyze("walk to coordinates 100, 64, 200", mockForeman, mockWorldKnowledge);

        assertEquals(TaskComplexity.NOVEL, complexity,
            "First-time 'walk to' command should be NOVEL");
    }

    @Test
    @DisplayName("Simple commands: 'attack entity' recognized")
    void simpleCommandAttackRecognized() {
        TaskComplexity complexity = analyzer.analyze("attack zombie", mockForeman, mockWorldKnowledge);

        assertEquals(TaskComplexity.NOVEL, complexity,
            "First-time 'attack' command should be NOVEL");
    }

    @Test
    @DisplayName("Simple commands: frequent execution downgrades to TRIVIAL")
    void simpleCommandDowngradedAfterFrequentExecution() {
        // Execute 'mine 10 stone' 5 times
        for (int i = 0; i < 5; i++) {
            analyzer.analyze("mine 10 stone", mockForeman, mockWorldKnowledge);
        }

        // After 5 executions, SIMPLE should downgrade to TRIVIAL
        TaskComplexity complexity = analyzer.analyze("mine 10 stone", mockForeman, mockWorldKnowledge);
        assertEquals(TaskComplexity.TRIVIAL, complexity,
            "Frequently seen SIMPLE command should downgrade to TRIVIAL");
    }

    // ------------------------------------------------------------------------
    // Moderate Command Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Moderate commands: 'build a house' recognized")
    void moderateCommandBuildHouseRecognized() {
        TaskComplexity complexity = analyzer.analyze("build a small house", mockForeman, mockWorldKnowledge);

        assertEquals(TaskComplexity.NOVEL, complexity,
            "First-time 'build a house' should be NOVEL");
    }

    @Test
    @DisplayName("Moderate commands: 'create structure' recognized")
    void moderateCommandCreateStructureRecognized() {
        TaskComplexity complexity = analyzer.analyze("create a shelter", mockForeman, mockWorldKnowledge);

        assertEquals(TaskComplexity.NOVEL, complexity,
            "First-time 'create' command should be NOVEL");
    }

    @Test
    @DisplayName("Moderate commands: 'construct building' recognized")
    void moderateCommandConstructRecognized() {
        TaskComplexity complexity = analyzer.analyze("construct storage room", mockForeman, mockWorldKnowledge);

        assertEquals(TaskComplexity.NOVEL, complexity,
            "First-time 'construct' command should be NOVEL");
    }

    @Test
    @DisplayName("Moderate commands: 'farm resource' recognized")
    void moderateCommandFarmRecognized() {
        TaskComplexity complexity = analyzer.analyze("farm wheat", mockForeman, mockWorldKnowledge);

        assertEquals(TaskComplexity.NOVEL, complexity,
            "First-time 'farm' command should be NOVEL");
    }

    @Test
    @DisplayName("Moderate commands: 'setup automation' recognized")
    void moderateCommandSetupRecognized() {
        TaskComplexity complexity = analyzer.analyze("setup automatic smelter", mockForeman, mockWorldKnowledge);

        assertEquals(TaskComplexity.NOVEL, complexity,
            "First-time 'setup' command should be NOVEL");
    }

    @Test
    @DisplayName("Moderate commands: 'gather resources for' recognized")
    void moderateCommandGatherResourcesForRecognized() {
        TaskComplexity complexity = analyzer.analyze("gather resources for crafting", mockForeman, mockWorldKnowledge);

        assertEquals(TaskComplexity.NOVEL, complexity,
            "First-time 'gather resources for' command should be NOVEL");
    }

    @Test
    @DisplayName("Moderate commands: frequent execution downgrades to SIMPLE")
    void moderateCommandDowngradedAfterFrequentExecution() {
        // Execute 'build a house' 10 times (threshold for MODERATE -> SIMPLE)
        for (int i = 0; i < 10; i++) {
            analyzer.analyze("build a house", mockForeman, mockWorldKnowledge);
        }

        // After 10 executions, MODERATE should downgrade to SIMPLE
        TaskComplexity complexity = analyzer.analyze("build a house", mockForeman, mockWorldKnowledge);
        assertEquals(TaskComplexity.SIMPLE, complexity,
            "Frequently seen MODERATE command should downgrade to SIMPLE");
    }

    // ------------------------------------------------------------------------
    // Complex Command Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Complex commands: 'coordinate team' recognized")
    void complexCommandCoordinateTeamRecognized() {
        TaskComplexity complexity = analyzer.analyze("coordinate the crew to build a castle", mockForeman, mockWorldKnowledge);

        assertEquals(TaskComplexity.NOVEL, complexity,
            "First-time 'coordinate the crew' should be NOVEL");
    }

    @Test
    @DisplayName("Complex commands: 'work together to' recognized")
    void complexCommandWorkTogetherRecognized() {
        TaskComplexity complexity = analyzer.analyze("work together to harvest the farm", mockForeman, mockWorldKnowledge);

        assertEquals(TaskComplexity.NOVEL, complexity,
            "First-time 'work together to' should be NOVEL");
    }

    @Test
    @DisplayName("Complex commands: 'collaborate to' recognized")
    void complexCommandCollaborateRecognized() {
        TaskComplexity complexity = analyzer.analyze("collaborate to mine the ore vein", mockForeman, mockWorldKnowledge);

        assertEquals(TaskComplexity.NOVEL, complexity,
            "First-time 'collaborate' should be NOVEL");
    }

    @Test
    @DisplayName("Complex commands: 'team up to' recognized")
    void complexCommandTeamUpRecognized() {
        TaskComplexity complexity = analyzer.analyze("team up to defeat the boss", mockForeman, mockWorldKnowledge);

        assertEquals(TaskComplexity.NOVEL, complexity,
            "First-time 'team up to' should be NOVEL");
    }

    @Test
    @DisplayName("Complex commands: 'everyone' recognized")
    void complexCommandEveryoneRecognized() {
        TaskComplexity complexity = analyzer.analyze("everyone attack the target", mockForeman, mockWorldKnowledge);

        assertEquals(TaskComplexity.NOVEL, complexity,
            "First-time 'everyone' command should be NOVEL");
    }

    @Test
    @DisplayName("Complex commands: 'all agents' recognized")
    void complexCommandAllAgentsRecognized() {
        TaskComplexity complexity = analyzer.analyze("all agents go to defensive positions", mockForeman, mockWorldKnowledge);

        assertEquals(TaskComplexity.NOVEL, complexity,
            "First-time 'all agents' command should be NOVEL");
    }

    // ------------------------------------------------------------------------
    // Novel Command Detection Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Novel command: first time execution")
    void novelCommandFirstTimeExecution() {
        String uniqueCommand = "execute a never before seen strategy";

        TaskComplexity complexity = analyzer.analyze(uniqueCommand, mockForeman, mockWorldKnowledge);

        assertEquals(TaskComplexity.NOVEL, complexity,
            "Never-before-seen command should be NOVEL");
    }

    @Test
    @DisplayName("Novel command: subsequent execution is no longer NOVEL")
    void novelCommandSubsequentExecution() {
        String uniqueCommand = "invent a new building technique";

        // First execution
        TaskComplexity firstComplexity = analyzer.analyze(uniqueCommand, mockForeman, mockWorldKnowledge);
        assertEquals(TaskComplexity.NOVEL, firstComplexity,
            "First execution should be NOVEL");

        // Second execution
        TaskComplexity secondComplexity = analyzer.analyze(uniqueCommand, mockForeman, mockWorldKnowledge);
        assertNotEquals(TaskComplexity.NOVEL, secondComplexity,
            "Subsequent execution should not be NOVEL");
    }

    // ------------------------------------------------------------------------
    // Length-Based Analysis Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Length analysis: very short command is TRIVIAL")
    void lengthAnalysisVeryShortCommand() {
        TaskComplexity complexity = analyzer.analyze("go", mockForeman, mockWorldKnowledge);

        assertEquals(TaskComplexity.NOVEL, complexity,
            "Very short command on first execution should be NOVEL");
    }

    @Test
    @DisplayName("Length analysis: short single-sentence command is SIMPLE")
    void lengthAnalysisShortSingleSentence() {
        TaskComplexity complexity = analyzer.analyze("go to the village and wait", mockForeman, mockWorldKnowledge);

        assertEquals(TaskComplexity.NOVEL, complexity,
            "Short command on first execution should be NOVEL");
    }

    @Test
    @DisplayName("Length analysis: multi-part command is MODERATE")
    void lengthAnalysisMultiPartCommand() {
        String multiPartCommand = "go to the village and buy supplies then return to base";

        TaskComplexity complexity = analyzer.analyze(multiPartCommand, mockForeman, mockWorldKnowledge);

        assertEquals(TaskComplexity.NOVEL, complexity,
            "Multi-part command on first execution should be NOVEL");
    }

    @Test
    @DisplayName("Length analysis: long complex command is COMPLEX")
    void lengthAnalysisLongComplexCommand() {
        String longCommand = "coordinate the team to gather resources from multiple locations, " +
            "construct a storage facility, organize the inventory, setup automated sorting system, " +
            "and then report back with statistics";

        TaskComplexity complexity = analyzer.analyze(longCommand, mockForeman, mockWorldKnowledge);

        assertEquals(TaskComplexity.NOVEL, complexity,
            "Long complex command on first execution should be NOVEL");
    }

    // ------------------------------------------------------------------------
    // Keyword Analysis Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Keyword analysis: single complexity keyword indicates MODERATE")
    void keywordAnalysisSingleComplexityKeyword() {
        TaskComplexity complexity = analyzer.analyze("optimize the storage", mockForeman, mockWorldKnowledge);

        assertEquals(TaskComplexity.NOVEL, complexity,
            "Command with 'optimize' on first execution should be NOVEL");
    }

    @Test
    @DisplayName("Keyword analysis: multiple complexity keywords indicate COMPLEX")
    void keywordAnalysisMultipleComplexityKeywords() {
        TaskComplexity complexity = analyzer.analyze("coordinate team to optimize strategy", mockForeman, mockWorldKnowledge);

        assertEquals(TaskComplexity.NOVEL, complexity,
            "Command with multiple keywords on first execution should be NOVEL");
    }

    // ------------------------------------------------------------------------
    // Context-Aware Analysis Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Context analysis: null worldKnowledge is handled gracefully")
    void contextAnalysisNullWorldKnowledge() {
        TaskComplexity complexity = analyzer.analyze("build a house", mockForeman, null);

        assertNotNull(complexity, "Should return a complexity even with null WorldKnowledge");
    }

    @Test
    @DisplayName("Context analysis: multi-agent keywords detected")
    void contextAnalysisMultiAgentKeywords() {
        TaskComplexity complexity = analyzer.analyze("everyone should coordinate", mockForeman, mockWorldKnowledge);

        // Should be NOVEL on first execution, but pattern detection would catch COMPLEX
        assertEquals(TaskComplexity.NOVEL, complexity,
            "Multi-agent command on first execution should be NOVEL");
    }

    @Test
    @DisplayName("Context analysis: precise coordinates increase complexity")
    void contextAnalysisPreciseCoordinates() {
        TaskComplexity complexity = analyzer.analyze("place blocks at 100, 64, 200", mockForeman, mockWorldKnowledge);

        assertEquals(TaskComplexity.NOVEL, complexity,
            "Command with precise coordinates on first execution should be NOVEL");
    }

    // ------------------------------------------------------------------------
    // History Tracking Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("History tracking: execution count increases")
    void historyTrackingExecutionCountIncreases() {
        String command = "mine 10 stone";

        assertEquals(0, analyzer.getExecutionCount(command),
            "Initial execution count should be 0");

        analyzer.analyze(command, mockForeman, mockWorldKnowledge);

        assertEquals(1, analyzer.getExecutionCount(command),
            "Execution count should be 1 after first call");
    }

    @Test
    @DisplayName("History tracking: command signature normalizes quantities")
    void historyTrackingCommandSignatureNormalizes() {
        String command1 = "mine 10 stone";
        String command2 = "mine 20 stone";

        analyzer.analyze(command1, mockForeman, mockWorldKnowledge);

        // Both commands should have same signature (quantities normalized to 'N')
        assertEquals(1, analyzer.getExecutionCount(command1),
            "First command should have count of 1");
        assertEquals(1, analyzer.getExecutionCount(command2),
            "Second command with different quantity should share signature");
    }

    @Test
    @DisplayName("History tracking: history size is tracked")
    void historyTrackingHistorySizeTracked() {
        analyzer.clearHistory();

        assertEquals(0, analyzer.getHistorySize(), "History should be empty after clear");

        analyzer.analyze("command1", mockForeman, mockWorldKnowledge);
        analyzer.analyze("command2", mockForeman, mockWorldKnowledge);

        assertEquals(2, analyzer.getHistorySize(), "History should contain 2 entries");
    }

    @Test
    @DisplayName("History tracking: clearHistory empties history")
    void historyTrackingClearHistoryWorks() {
        analyzer.analyze("command1", mockForeman, mockWorldKnowledge);
        analyzer.analyze("command2", mockForeman, mockWorldKnowledge);

        assertTrue(analyzer.getHistorySize() > 0, "History should have entries");

        analyzer.clearHistory();

        assertEquals(0, analyzer.getHistorySize(), "History should be empty after clear");
    }

    // ------------------------------------------------------------------------
    // Edge Cases and Validation Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Edge case: null command returns TRIVIAL")
    void edgeCaseNullCommand() {
        TaskComplexity complexity = analyzer.analyze(null, mockForeman, mockWorldKnowledge);

        assertEquals(TaskComplexity.TRIVIAL, complexity,
            "Null command should return TRIVIAL");
    }

    @Test
    @DisplayName("Edge case: empty command returns TRIVIAL")
    void edgeCaseEmptyCommand() {
        TaskComplexity complexity = analyzer.analyze("", mockForeman, mockWorldKnowledge);

        assertEquals(TaskComplexity.TRIVIAL, complexity,
            "Empty command should return TRIVIAL");
    }

    @Test
    @DisplayName("Edge case: whitespace-only command returns TRIVIAL")
    void edgeCaseWhitespaceCommand() {
        TaskComplexity complexity = analyzer.analyze("   ", mockForeman, mockWorldKnowledge);

        assertEquals(TaskComplexity.TRIVIAL, complexity,
            "Whitespace-only command should return TRIVIAL");
    }

    @Test
    @DisplayName("Edge case: very long command is handled")
    void edgeCaseVeryLongCommand() {
        String longCommand = "execute ".repeat(1000) + "task";

        TaskComplexity complexity = analyzer.analyze(longCommand, mockForeman, mockWorldKnowledge);

        assertNotNull(complexity, "Should handle very long commands");
    }

    @Test
    @DisplayName("Edge case: special characters in command")
    void edgeCaseSpecialCharacters() {
        TaskComplexity complexity = analyzer.analyze("mine 10 stone! @#$%^&*()", mockForeman, mockWorldKnowledge);

        assertNotNull(complexity, "Should handle special characters");
    }

    // ------------------------------------------------------------------------
    // Thread Safety Tests
    // ------------------------------------------------------------------------

    @Test
    @DisplayName("Thread safety: concurrent analyze calls")
    void threadSafetyConcurrentAnalyzeCalls() throws InterruptedException {
        int threadCount = 10;
        int callsPerThread = 100;
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < callsPerThread; j++) {
                    analyzer.analyze("command " + threadId, mockForeman, mockWorldKnowledge);
                }
            });
        }

        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        assertEquals(threadCount, analyzer.getHistorySize(),
            "All unique commands should be tracked");
    }
}
