package com.minewright.dialogue;

import com.minewright.memory.CompanionMemory;
import com.minewright.memory.PersonalitySystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages speech patterns for personality consistency in dialogue.
 * <p>
 * Responsible for:
 * <ul>
 *   <li>Tracking phrase usage to avoid repetition</li>
 *   <li>Applying personality-based verbal tics</li>
 *   <li>Adding personality-based endings and modifiers</li>
 *   <li>Maintaining recent phrase history</li>
 * </ul>
 *
 * @since 1.4.0
 */
public class DialogueSpeechPatternManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(DialogueSpeechPatternManager.class);

    private final CompanionMemory memory;
    private final Random random;

    // Speech pattern tracking for personality consistency
    private final Map<String, Integer> phraseUsageCount;
    private final Queue<String> recentPhrases;

    public DialogueSpeechPatternManager(CompanionMemory memory) {
        this.memory = memory;
        this.random = new Random();
        this.phraseUsageCount = new ConcurrentHashMap<>();
        this.recentPhrases = new LinkedList<>();
    }

    /**
     * Tracks speech patterns for personality consistency.
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
     */
    public double getSpeechPatternPenalty(String triggerType) {
        int usageCount = phraseUsageCount.getOrDefault(triggerType, 0);
        // Slight penalty for each use, caps at 20%
        return Math.min(0.2, usageCount * 0.05);
    }

    /**
     * Checks if a phrase was used too recently.
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
     * Applies speech patterns to make dialogue more natural and personality-consistent.
     */
    public String applySpeechPattern(String comment, String triggerType) {
        if (comment == null || comment.isEmpty()) {
            return comment;
        }

        PersonalitySystem.PersonalityProfile personality = memory.getPersonality();

        // Add verbal tics based on personality
        String verbalTic = getVerbalTic(personality, triggerType);
        if (verbalTic != null && !verbalTic.isEmpty() && random.nextFloat() < 0.3) {
            // 30% chance to add verbal tic
            comment = verbalTic + " " + comment;
        }

        // Add personality-based endings
        if (personality.getExtraversion() > 70 && random.nextFloat() < 0.2) {
            // High extraversion: occasionally add enthusiastic endings
            String[] enthusiasticEndings = {"!", "!", "!"};
            if (!comment.endsWith("!")) {
                comment += enthusiasticEndings[random.nextInt(enthusiasticEndings.length)];
            }
        } else if (personality.getFormality() > 60 && random.nextFloat() < 0.15) {
            // High formality: occasionally add polite endings
            String[] politeEndings = {", if you please.", ", at your service."};
            comment += politeEndings[random.nextInt(politeEndings.length)];
        }

        return comment;
    }

    /**
     * Gets a verbal tic based on personality traits.
     */
    private String getVerbalTic(PersonalitySystem.PersonalityProfile personality, String triggerType) {
        // Select verbal tic based on personality and context
        if (personality.getHumor() > 60 && random.nextFloat() < 0.25) {
            String[] humorousTics = {
                "Well,",
                "You see,",
                "Funny thing is,"
            };
            return humorousTics[random.nextInt(humorousTics.length)];
        } else if (personality.getConscientiousness() > 70 && random.nextFloat() < 0.2) {
            String[] conscientiousTics = {
                "Now then,",
                "Right then,",
                "Let's see,"
            };
            return conscientiousTics[random.nextInt(conscientiousTics.length)];
        } else if (personality.getExtraversion() > 70 && random.nextFloat() < 0.25) {
            String[] extravertedTics = {
                "Hey!",
                "Oh!",
                "Ah,",
                "Well then,"
            };
            return extravertedTics[random.nextInt(extravertedTics.length)];
        }
        return null;
    }

    /**
     * Gets the phrase usage count map.
     */
    public Map<String, Integer> getPhraseUsageCount() {
        return new ConcurrentHashMap<>(phraseUsageCount);
    }

    /**
     * Clears speech pattern history (for testing/debugging).
     */
    public void clearHistory() {
        phraseUsageCount.clear();
        recentPhrases.clear();
        LOGGER.debug("Speech pattern history cleared");
    }
}
