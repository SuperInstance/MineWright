package com.minewright.dialogue;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks speech patterns for personality consistency and variety.
 *
 * <p>This class monitors how often different phrases are used to ensure
 * that the agent doesn't repeat the same dialogue too frequently. It
 * maintains usage counts and recent phrase history for pattern analysis.</p>
 *
 * @since 1.3.0
 */
public class SpeechPatternTracker {

    // Speech pattern tracking for personality consistency
    private final Map<String, Integer> phraseUsageCount;
    private final Queue<String> recentPhrases;

    public SpeechPatternTracker() {
        this.phraseUsageCount = new ConcurrentHashMap<>();
        this.recentPhrases = new LinkedList<>();
    }

    /**
     * Tracks a speech pattern for personality consistency.
     *
     * @param triggerType The trigger type that was used
     */
    public void trackSpeechPattern(String triggerType) {
        phraseUsageCount.merge(triggerType, 1, Integer::sum);
        recentPhrases.offer(triggerType);
        if (recentPhrases.size() > 10) {
            recentPhrases.poll();
        }
    }

    /**
     * Gets a penalty for speech pattern repetition.
     * Reduces chance of using the same phrase too frequently.
     *
     * @param triggerType The trigger type to check
     * @return Penalty value from 0.0 to 0.2
     */
    public double getSpeechPatternPenalty(String triggerType) {
        int usageCount = phraseUsageCount.getOrDefault(triggerType, 0);
        // Slight penalty for each use, caps at 20%
        return Math.min(0.2, usageCount * 0.05);
    }

    /**
     * Checks if a phrase was used too recently.
     *
     * @param triggerType The trigger type to check
     * @return true if the phrase was used too recently
     */
    public boolean isPhraseTooRecent(String triggerType) {
        if (recentPhrases.size() < 3) {
            return false;
        }
        // Check if this trigger type was used in the last 3 dialogues
        int recentCount = 0;
        for (String phrase : recentPhrases) {
            if (phrase.equals(triggerType)) {
                recentCount++;
            }
        }
        return recentCount >= 2;
    }

    /**
     * Gets the speech pattern description for a trigger type.
     *
     * @param triggerType The trigger type to describe
     * @return Description of how often this trigger is used
     */
    public String getSpeechPatternForTrigger(String triggerType) {
        // Return how often this trigger is used
        int count = phraseUsageCount.getOrDefault(triggerType, 0);
        if (count == 0) return "new topic";
        if (count < 3) return "occasional topic";
        if (count < 6) return "regular topic";
        return "frequent topic";
    }

    /**
     * Gets the number of times a trigger has been used.
     *
     * @param triggerType The trigger type to check
     * @return Usage count
     */
    public int getUsageCount(String triggerType) {
        return phraseUsageCount.getOrDefault(triggerType, 0);
    }

    /**
     * Gets all phrase usage counts.
     *
     * @return Map of trigger types to usage counts
     */
    public Map<String, Integer> getPhraseUsageCount() {
        return new ConcurrentHashMap<>(phraseUsageCount);
    }

    /**
     * Gets the recent phrase history.
     *
     * @return Queue of recent phrases
     */
    public Queue<String> getRecentPhrases() {
        return new LinkedList<>(recentPhrases);
    }

    /**
     * Resets the tracking data (for testing).
     */
    public void reset() {
        phraseUsageCount.clear();
        recentPhrases.clear();
    }
}
