package com.minewright.dialogue;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages cooldowns and frequency control for proactive dialogue.
 *
 * <p>This class ensures that dialogue doesn't spam the player by tracking
 * cooldown periods for different trigger types and managing the overall
 * frequency of comments.</p>
 *
 * @since 1.3.0
 */
public class DialogueCooldownManager {

    // Cooldown tracking per trigger type
    private final Map<String, Long> triggerCooldowns;

    // State tracking
    private int ticksSinceLastComment = 0;
    private long lastCommentTimestamp = 0;
    private String lastCommentType = null;

    // Configuration
    private final int baseCooldownTicks;

    public DialogueCooldownManager(int baseCooldownTicks) {
        this.baseCooldownTicks = baseCooldownTicks;
        this.triggerCooldowns = new HashMap<>();
    }

    /**
     * Checks if a trigger can fire based on cooldown.
     *
     * @param triggerType The trigger to check
     * @param cooldownTicks Minimum ticks between triggers
     * @return true if the trigger can fire
     */
    public boolean canTrigger(String triggerType, int cooldownTicks) {
        Long lastTrigger = triggerCooldowns.get(triggerType);
        if (lastTrigger == null) {
            return true;
        }

        long ticksSinceTrigger = ticksSinceLastComment;
        return ticksSinceTrigger >= cooldownTicks;
    }

    /**
     * Records that a trigger has fired.
     *
     * @param triggerType The trigger that fired
     */
    public void recordTrigger(String triggerType) {
        triggerCooldowns.put(triggerType, System.currentTimeMillis());
    }

    /**
     * Records that a comment was spoken.
     *
     * @param triggerType The type of comment
     */
    public void recordComment(String triggerType) {
        lastCommentType = triggerType;
        lastCommentTimestamp = System.currentTimeMillis();
        ticksSinceLastComment = 0;
    }

    /**
     * Increments the tick counter.
     */
    public void incrementTick() {
        ticksSinceLastComment++;
    }

    /**
     * Checks if the same comment type was used recently.
     *
     * @param triggerType The trigger type to check
     * @return true if the same type was used recently
     */
    public boolean isSameTypeRecent(String triggerType) {
        return triggerType.equals(lastCommentType) && ticksSinceLastComment < baseCooldownTicks;
    }

    /**
     * Gets the number of ticks since the last comment.
     *
     * @return Ticks since last comment
     */
    public int getTicksSinceLastComment() {
        return ticksSinceLastComment;
    }

    /**
     * Gets the timestamp of the last comment.
     *
     * @return Last comment timestamp
     */
    public long getLastCommentTimestamp() {
        return lastCommentTimestamp;
    }

    /**
     * Gets the type of the last comment.
     *
     * @return Last comment type
     */
    public String getLastCommentType() {
        return lastCommentType;
    }

    /**
     * Resets the cooldown state (for testing).
     */
    public void reset() {
        triggerCooldowns.clear();
        ticksSinceLastComment = 0;
        lastCommentTimestamp = 0;
        lastCommentType = null;
    }
}
