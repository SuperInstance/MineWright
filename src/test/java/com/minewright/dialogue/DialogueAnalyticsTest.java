package com.minewright.dialogue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link DialogueAnalytics}.
 *
 * <p>Tests cover decision recording, statistics generation, trigger/skip tracking,
 * and history management.</p>
 *
 * @since 1.3.0
 */
@DisplayName("DialogueAnalytics Tests")
class DialogueAnalyticsTest {

    private DialogueAnalytics analytics;

    @BeforeEach
    void setUp() {
        analytics = new DialogueAnalytics();
    }

    // ========== Constructor Tests ==========

    @Test
    @DisplayName("Constructor should initialize with zero counts")
    void testConstructorInitializesZeroCounts() {
        assertEquals(0, analytics.getTotalTriggered(),
            "Total triggered should start at 0");
        assertEquals(0, analytics.getTotalSkipped(),
            "Total skipped should start at 0");
        assertTrue(analytics.getDialogueHistory().isEmpty(),
            "History should start empty");
    }

    // ========== recordDecision Tests ==========

    @Test
    @DisplayName("recordDecision should add decision to history")
    void testRecordDecisionAddsToHistory() {
        DialogueAnalytics.DialogueDecision decision = new DialogueAnalytics.DialogueDecision(
            "greeting", "test context", true, 0.5, 50, Instant.now()
        );

        analytics.recordDecision(decision);

        assertEquals(1, analytics.getDialogueHistory().size(),
            "History should contain one decision");
        assertEquals(decision, analytics.getDialogueHistory().get(0),
            "Recorded decision should match");
    }

    @Test
    @DisplayName("recordDecision should handle multiple decisions")
    void testRecordDecisionMultipleDecisions() {
        for (int i = 0; i < 5; i++) {
            analytics.recordDecision(new DialogueAnalytics.DialogueDecision(
                "test" + i, "context" + i, i % 2 == 0, 0.5, 50, Instant.now()
            ));
        }

        assertEquals(5, analytics.getDialogueHistory().size(),
            "All decisions should be recorded");
    }

    @Test
    @DisplayName("getDialogueHistory should return unmodifiable list")
    void testGetDialogueHistoryUnmodifiable() {
        analytics.recordDecision(new DialogueAnalytics.DialogueDecision(
            "test", "context", true, 0.5, 50, Instant.now()
        ));

        var history = analytics.getDialogueHistory();

        assertThrows(UnsupportedOperationException.class,
            () -> history.add(new DialogueAnalytics.DialogueDecision(
                "test2", "context2", true, 0.5, 50, Instant.now()
            )),
            "History should be unmodifiable");
    }

    // ========== recordTriggered Tests ==========

    @Test
    @DisplayName("recordTriggered should increment counter")
    void testRecordTriggeredIncrementsCounter() {
        analytics.recordTriggered();
        assertEquals(1, analytics.getTotalTriggered());

        analytics.recordTriggered();
        analytics.recordTriggered();
        assertEquals(3, analytics.getTotalTriggered(),
            "Should increment with each call");
    }

    // ========== recordSkipped Tests ==========

    @Test
    @DisplayName("recordSkipped should increment counter")
    void testRecordSkippedIncrementsCounter() {
        analytics.recordSkipped();
        assertEquals(1, analytics.getTotalSkipped());

        analytics.recordSkipped();
        analytics.recordSkipped();
        assertEquals(3, analytics.getTotalSkipped(),
            "Should increment with each call");
    }

    // ========== getStatistics Tests ==========

    @Test
    @DisplayName("getStatistics should return correct trigger rate")
    void testGetStatisticsTriggerRate() {
        analytics.recordTriggered();
        analytics.recordTriggered();
        analytics.recordSkipped();

        Map<String, Integer> phraseUsage = Map.of(
            "greeting", 5,
            "farewell", 3
        );

        DialogueAnalytics.DialogueStatistics stats = analytics.getStatistics(phraseUsage);

        assertEquals(2, stats.totalTriggered);
        assertEquals(1, stats.totalSkipped);
        assertEquals(0.666, stats.getTriggerRate(), 0.01,
            "Trigger rate should be ~66.7%");
    }

    @Test
    @DisplayName("getStatistics should handle zero total")
    void testGetStatisticsZeroTotal() {
        Map<String, Integer> phraseUsage = Map.of();
        DialogueAnalytics.DialogueStatistics stats = analytics.getStatistics(phraseUsage);

        assertEquals(0.0, stats.getTriggerRate(), 0.001,
            "Trigger rate should be 0.0 when no dialogues");
    }

    @Test
    @DisplayName("getStatistics should include phrase usage")
    void testGetStatisticsIncludesPhraseUsage() {
        analytics.recordTriggered();

        Map<String, Integer> phraseUsage = Map.of(
            "greeting", 10,
            "farewell", 5
        );

        DialogueAnalytics.DialogueStatistics stats = analytics.getStatistics(phraseUsage);

        assertEquals(phraseUsage, stats.triggerUsage,
            "Phrase usage should match");
        assertEquals(2, stats.triggerUsage.size(),
            "Should include all phrase types");
    }

    @Test
    @DisplayName("getStatistics should include history size")
    void testGetStatisticsIncludesHistorySize() {
        analytics.recordDecision(new DialogueAnalytics.DialogueDecision(
            "test", "context", true, 0.5, 50, Instant.now()
        ));
        analytics.recordDecision(new DialogueAnalytics.DialogueDecision(
            "test2", "context2", false, 0.3, 40, Instant.now()
        ));

        Map<String, Integer> phraseUsage = Map.of();
        DialogueAnalytics.DialogueStatistics stats = analytics.getStatistics(phraseUsage);

        assertEquals(2, stats.historySize,
            "History size should match number of decisions");
    }

    // ========== clearHistory Tests ==========

    @Test
    @DisplayName("clearHistory should remove all decisions")
    void testClearHistoryRemovesDecisions() {
        analytics.recordDecision(new DialogueAnalytics.DialogueDecision(
            "test", "context", true, 0.5, 50, Instant.now()
        ));
        analytics.recordDecision(new DialogueAnalytics.DialogueDecision(
            "test2", "context2", false, 0.3, 40, Instant.now()
        ));

        assertEquals(2, analytics.getDialogueHistory().size());

        analytics.clearHistory();

        assertTrue(analytics.getDialogueHistory().isEmpty(),
            "History should be empty after clear");
    }

    @Test
    @DisplayName("clearHistory should not affect counters")
    void testClearHistoryPreservesCounters() {
        analytics.recordTriggered();
        analytics.recordSkipped();
        analytics.recordDecision(new DialogueAnalytics.DialogueDecision(
            "test", "context", true, 0.5, 50, Instant.now()
        ));

        analytics.clearHistory();

        assertEquals(1, analytics.getTotalTriggered(),
            "Triggered count should be preserved");
        assertEquals(1, analytics.getTotalSkipped(),
            "Skipped count should be preserved");
    }

    // ========== reset Tests ==========

    @Test
    @DisplayName("reset should clear all state")
    void testResetClearsAll() {
        analytics.recordTriggered();
        analytics.recordSkipped();
        analytics.recordDecision(new DialogueAnalytics.DialogueDecision(
            "test", "context", true, 0.5, 50, Instant.now()
        ));

        analytics.reset();

        assertEquals(0, analytics.getTotalTriggered(),
            "Triggered count should be reset");
        assertEquals(0, analytics.getTotalSkipped(),
            "Skipped count should be reset");
        assertTrue(analytics.getDialogueHistory().isEmpty(),
            "History should be empty");
    }

    // ========== DialogueDecision Tests ==========

    @Test
    @DisplayName("DialogueDecision should store all fields")
    void testDialogueDecisionStoresFields() {
        Instant now = Instant.now();
        DialogueAnalytics.DialogueDecision decision = new DialogueAnalytics.DialogueDecision(
            "greeting", "approached player", true, 0.75, 65, now
        );

        assertEquals("greeting", decision.triggerType);
        assertEquals("approached player", decision.context);
        assertTrue(decision.wasTriggered);
        assertEquals(0.75, decision.triggerChance, 0.001);
        assertEquals(65, decision.rapportAtTime);
        assertEquals(now, decision.timestamp);
    }

    @Test
    @DisplayName("DialogueDecision toString should format correctly")
    void testDialogueDecisionToString() {
        Instant now = Instant.parse("2024-01-01T12:00:00Z");
        DialogueAnalytics.DialogueDecision decision = new DialogueAnalytics.DialogueDecision(
            "greeting", "test", true, 0.85, 70, now
        );

        String result = decision.toString();

        assertTrue(result.contains("[greeting]"));
        assertTrue(result.contains("test"));
        assertTrue(result.contains("0.85"));
        assertTrue(result.contains("true"));
        assertTrue(result.contains("70"));
    }

    // ========== DialogueStatistics Tests ==========

    @Test
    @DisplayName("DialogueStatistics should store all fields")
    void testDialogueStatisticsStoresFields() {
        Map<String, Integer> usage = Map.of("greeting", 5, "farewell", 3);
        DialogueAnalytics.DialogueStatistics stats = new DialogueAnalytics.DialogueStatistics(
            10, 5, usage, 15
        );

        assertEquals(10, stats.totalTriggered);
        assertEquals(5, stats.totalSkipped);
        assertEquals(usage, stats.triggerUsage);
        assertEquals(15, stats.historySize);
    }

    @Test
    @DisplayName("DialogueStatistics getTriggerRate should calculate correctly")
    void testDialogueStatisticsGetTriggerRate() {
        Map<String, Integer> usage = Map.of();
        DialogueAnalytics.DialogueStatistics stats = new DialogueAnalytics.DialogueStatistics(
            7, 3, usage, 10
        );

        assertEquals(0.7, stats.getTriggerRate(), 0.001,
            "Trigger rate should be 70%");
    }

    @Test
    @DisplayName("DialogueStatistics getTriggerRate handles zero")
    void testDialogueStatisticsGetTriggerRateZero() {
        Map<String, Integer> usage = Map.of();
        DialogueAnalytics.DialogueStatistics stats = new DialogueAnalytics.DialogueStatistics(
            0, 0, usage, 0
        );

        assertEquals(0.0, stats.getTriggerRate(), 0.001,
            "Trigger rate should be 0.0 when no data");
    }

    @Test
    @DisplayName("DialogueStatistics toString should format correctly")
    void testDialogueStatisticsToString() {
        Map<String, Integer> usage = Map.of("greeting", 5);
        DialogueAnalytics.DialogueStatistics stats = new DialogueAnalytics.DialogueStatistics(
            8, 2, usage, 10
        );

        String result = stats.toString();

        assertTrue(result.contains("triggered=8"));
        assertTrue(result.contains("skipped=2"));
        assertTrue(result.contains("80.00%")); // 8/10 = 80%
        assertTrue(result.contains("triggers=1"));
    }

    // ========== Integration Tests ==========

    @Test
    @DisplayName("Full workflow: record decisions and generate statistics")
    void testFullWorkflow() {
        // Record some decisions
        analytics.recordDecision(new DialogueAnalytics.DialogueDecision(
            "greeting", "player approached", true, 0.8, 60, Instant.now()
        ));
        analytics.recordDecision(new DialogueAnalytics.DialogueDecision(
            "greeting", "player approached", false, 0.2, 60, Instant.now()
        ));
        analytics.recordDecision(new DialogueAnalytics.DialogueDecision(
            "task_complete", "finished mining", true, 0.9, 70, Instant.now()
        ));

        // Record some counts
        analytics.recordTriggered();
        analytics.recordTriggered();
        analytics.recordSkipped();

        // Get statistics
        Map<String, Integer> phraseUsage = Map.of(
            "greeting", 15,
            "task_complete", 8
        );
        DialogueAnalytics.DialogueStatistics stats = analytics.getStatistics(phraseUsage);

        // Verify
        assertEquals(2, stats.totalTriggered);
        assertEquals(1, stats.totalSkipped);
        assertEquals(0.666, stats.getTriggerRate(), 0.01);
        assertEquals(3, stats.historySize);
        assertEquals(2, stats.triggerUsage.size());
    }

    @Test
    @DisplayName("Statistics should reflect recent activity pattern")
    void testStatisticsReflectActivityPattern() {
        // Simulate active period
        for (int i = 0; i < 10; i++) {
            analytics.recordTriggered();
        }
        // Simulate quiet period
        for (int i = 0; i < 5; i++) {
            analytics.recordSkipped();
        }

        Map<String, Integer> phraseUsage = Map.of();
        DialogueAnalytics.DialogueStatistics stats = analytics.getStatistics(phraseUsage);

        assertEquals(10, stats.totalTriggered);
        assertEquals(5, stats.totalSkipped);
        assertEquals(0.666, stats.getTriggerRate(), 0.001,
            "Active period should show 66% trigger rate");
    }
}
