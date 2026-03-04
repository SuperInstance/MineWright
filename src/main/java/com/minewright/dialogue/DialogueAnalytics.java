package com.minewright.dialogue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Handles logging and statistics for proactive dialogue.
 *
 * <p>This class tracks dialogue decisions, generates statistics, and maintains
 * history for analysis of dialogue effectiveness and patterns.</p>
 *
 * @since 1.3.0
 */
public class DialogueAnalytics {

    // Dialogue decision logging
    private final List<DialogueDecision> dialogueHistory;
    private int totalDialoguesTriggered = 0;
    private int totalDialoguesSkipped = 0;

    public DialogueAnalytics() {
        this.dialogueHistory = new ArrayList<>();
    }

    /**
     * Records a dialogue decision.
     *
     * @param decision The decision to record
     */
    public void recordDecision(DialogueDecision decision) {
        dialogueHistory.add(decision);
    }

    /**
     * Records that a dialogue was triggered.
     */
    public void recordTriggered() {
        totalDialoguesTriggered++;
    }

    /**
     * Records that a dialogue was skipped.
     */
    public void recordSkipped() {
        totalDialoguesSkipped++;
    }

    /**
     * Gets the dialogue history for analysis.
     *
     * @return Unmodifiable list of dialogue decisions
     */
    public List<DialogueDecision> getDialogueHistory() {
        return Collections.unmodifiableList(dialogueHistory);
    }

    /**
     * Gets statistics about dialogue usage.
     *
     * @param phraseUsageCount The phrase usage count map
     * @return DialogueStatistics object with usage metrics
     */
    public DialogueStatistics getStatistics(Map<String, Integer> phraseUsageCount) {
        return new DialogueStatistics(
            totalDialoguesTriggered,
            totalDialoguesSkipped,
            phraseUsageCount,
            dialogueHistory.size()
        );
    }

    /**
     * Gets the total number of dialogues triggered.
     *
     * @return Total triggered count
     */
    public int getTotalTriggered() {
        return totalDialoguesTriggered;
    }

    /**
     * Gets the total number of dialogues skipped.
     *
     * @return Total skipped count
     */
    public int getTotalSkipped() {
        return totalDialoguesSkipped;
    }

    /**
     * Clears dialogue history (for testing/debugging).
     */
    public void clearHistory() {
        dialogueHistory.clear();
    }

    /**
     * Resets all statistics (for testing).
     */
    public void reset() {
        dialogueHistory.clear();
        totalDialoguesTriggered = 0;
        totalDialoguesSkipped = 0;
    }

    /**
     * Represents a single dialogue decision made by the system.
     * Used for logging and analysis of dialogue effectiveness.
     */
    public static class DialogueDecision {
        public final String triggerType;
        public final String context;
        public final boolean wasTriggered;
        public final double triggerChance;
        public final int rapportAtTime;
        public final Instant timestamp;

        public DialogueDecision(String triggerType, String context, boolean wasTriggered,
                             double triggerChance, int rapportAtTime, Instant timestamp) {
            this.triggerType = triggerType;
            this.context = context;
            this.wasTriggered = wasTriggered;
            this.triggerChance = triggerChance;
            this.rapportAtTime = rapportAtTime;
            this.timestamp = timestamp;
        }

        @Override
        public String toString() {
            return String.format("[%s] %s - Chance: %.2f, Triggered: %s, Rapport: %d",
                triggerType, context, triggerChance, wasTriggered, rapportAtTime);
        }
    }

    /**
     * Statistics about dialogue usage and effectiveness.
     */
    public static class DialogueStatistics {
        public final int totalTriggered;
        public final int totalSkipped;
        public final Map<String, Integer> triggerUsage;
        public final int historySize;

        public DialogueStatistics(int totalTriggered, int totalSkipped,
                                Map<String, Integer> triggerUsage, int historySize) {
            this.totalTriggered = totalTriggered;
            this.totalSkipped = totalSkipped;
            this.triggerUsage = triggerUsage;
            this.historySize = historySize;
        }

        public double getTriggerRate() {
            int total = totalTriggered + totalSkipped;
            return total > 0 ? (double) totalTriggered / total : 0.0;
        }

        @Override
        public String toString() {
            return String.format("DialogueStatistics{triggered=%d, skipped=%d, rate=%.2f%%, triggers=%d}",
                totalTriggered, totalSkipped, getTriggerRate() * 100, triggerUsage.size());
        }
    }
}
