package com.minewright.dialogue;

import com.minewright.entity.ForemanEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Detects environmental and event-based triggers for proactive dialogue.
 *
 * <p>This class is responsible for monitoring the game world and detecting
 * conditions that should trigger dialogue, such as:</p>
 *
 * <ul>
 *   <li>Time-based triggers (morning, night)</li>
 *   <li>Weather triggers (rain, storm)</li>
 *   <li>Context triggers (biome, danger)</li>
 *   <li>Proximity triggers (player approach)</li>
 * </ul>
 *
 * @since 1.3.0
 */
public class DialogueTriggerDetector {

    private static final Logger LOGGER = LoggerFactory.getLogger(DialogueTriggerDetector.class);

    private final ForemanEntity minewright;
    private final DialogueCooldownManager cooldownManager;

    // Weather state tracking
    private boolean wasRaining = false;
    private long lastWeatherCheckTime = 0;

    // Player greeting tracking
    private long lastGreetingTime = 0;
    private Player lastGreetedPlayer = null;

    public DialogueTriggerDetector(ForemanEntity minewright, DialogueCooldownManager cooldownManager) {
        this.minewright = minewright;
        this.cooldownManager = cooldownManager;
    }

    /**
     * Checks all time-based triggers (morning, night, idle).
     *
     * @param ticksSinceLastComment Ticks since the last dialogue
     * @param isExecuting Whether the entity is currently executing an action
     * @return TriggerResult if a trigger should fire, null otherwise
     */
    public TriggerResult checkTimeBasedTriggers(int ticksSinceLastComment, boolean isExecuting) {
        Level level = minewright.level();
        long dayTime = level.getDayTime() % 24000;

        // Morning greeting (6:00 AM game time)
        if (dayTime >= 0 && dayTime < 2000) {
            if (cooldownManager.canTrigger("morning", 24000)) {
                return new TriggerResult("morning", "It's morning!");
            }
        }

        // Night warning (8:00 PM game time)
        if (dayTime >= 18000 && dayTime < 20000) {
            if (cooldownManager.canTrigger("night", 12000)) {
                return new TriggerResult("night", "It's getting dark!");
            }
        }

        // Idle comment (if idle for too long)
        if (!isExecuting && ticksSinceLastComment > 1200) {
            if (cooldownManager.canTrigger("idle_long", 6000)) {
                return new TriggerResult("idle_long", "Been idle a while");
            }
        }

        return null;
    }

    /**
     * Checks context-based triggers (biome, location, danger).
     *
     * @return TriggerResult if a trigger should fire, null otherwise
     */
    public TriggerResult checkContextBasedTriggers() {
        if (minewright.level().isClientSide) {
            return null;
        }

        // Check biome
        try {
            String biome = minewright.level().getBiome(minewright.blockPosition()).unwrapKey().
                map(key -> key.location().getPath()).
                orElse("unknown");

            // Special biomes
            if (biome.contains("nether")) {
                if (cooldownManager.canTrigger("nether_biome", 3000)) {
                    return new TriggerResult("nether_biome", "Entered the Nether");
                }
            } else if (biome.contains("end")) {
                if (cooldownManager.canTrigger("end_biome", 3000)) {
                    return new TriggerResult("end_biome", "Entered the End");
                }
            } else if (biome.contains("snow") || biome.contains("ice")) {
                if (cooldownManager.canTrigger("cold_biome", 6000)) {
                    return new TriggerResult("cold_biome", "Entered cold biome");
                }
            } else if (biome.contains("desert")) {
                if (cooldownManager.canTrigger("desert_biome", 6000)) {
                    return new TriggerResult("desert_biome", "Entered desert");
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Error checking biome for proactive dialogue", e);
        }

        // Check for danger (low health, nearby hostile)
        if (minewright.getHealth() < minewright.getMaxHealth() * 0.3) {
            if (cooldownManager.canTrigger("low_health", 2000)) {
                return new TriggerResult("low_health", "Health is low!");
            }
        }

        return null;
    }

    /**
     * Checks weather-related triggers.
     *
     * @return TriggerResult if a trigger should fire, null otherwise
     */
    public TriggerResult checkWeatherTriggers() {
        Level level = minewright.level();

        // Check weather occasionally
        long now = System.currentTimeMillis();
        if (now - lastWeatherCheckTime < 5000) {
            return null;
        }
        lastWeatherCheckTime = now;

        boolean isRaining = level.isRaining();
        boolean isThundering = level.isThundering();

        // Rain started
        if (isRaining && !wasRaining) {
            if (isThundering) {
                if (cooldownManager.canTrigger("storm", 10000)) {
                    wasRaining = isRaining;
                    return new TriggerResult("storm", "Storm started!");
                }
            } else {
                if (cooldownManager.canTrigger("raining", 15000)) {
                    wasRaining = isRaining;
                    return new TriggerResult("raining", "Rain started!");
                }
            }
        }

        wasRaining = isRaining;
        return null;
    }

    /**
     * Checks player proximity triggers (greeting, approach).
     *
     * @return TriggerResult if a trigger should fire, null otherwise
     */
    public TriggerResult checkPlayerProximityTriggers() {
        Level level = minewright.level();
        if (level.isClientSide) {
            return null;
        }

        // Find nearest player
        Player nearestPlayer = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Player player : level.players()) {
            double dist = minewright.position().distanceTo(player.position());
            if (dist < nearestDistance) {
                nearestDistance = dist;
                nearestPlayer = player;
            }
        }

        if (nearestPlayer == null) {
            return null;
        }

        // Greet when player approaches (within 10 blocks)
        if (nearestDistance < 10.0) {
            long now = System.currentTimeMillis();

            // Check if we should greet
            boolean shouldGreet = false;
            if (lastGreetedPlayer == null || lastGreetedPlayer != nearestPlayer) {
                shouldGreet = true;
            } else if (now - lastGreetingTime > 60000) {  // 1 minute cooldown
                shouldGreet = true;
            }

            if (shouldGreet) {
                lastGreetedPlayer = nearestPlayer;
                lastGreetingTime = now;
                return new TriggerResult("player_approach", "Player approached");
            }
        }

        return null;
    }

    /**
     * Represents a detected trigger ready to fire.
     */
    public static class TriggerResult {
        private final String triggerType;
        private final String context;

        public TriggerResult(String triggerType, String context) {
            this.triggerType = triggerType;
            this.context = context;
        }

        public String getTriggerType() {
            return triggerType;
        }

        public String getContext() {
            return context;
        }
    }
}
