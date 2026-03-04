package com.minewright.dialogue;

import com.minewright.entity.ForemanEntity;
import com.minewright.memory.CompanionMemory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Checks for proactive dialogue triggers based on game state.
 * <p>
 * Responsible for evaluating whether dialogue should be triggered based on:
 * <ul>
 *   <li>Time of day (morning, night)</li>
 *   <li>Weather conditions (rain, storms)</li>
 *   <li>Biome and location</li>
 *   <li>Player proximity</li>
 *   <li>Agent health and danger</li>
 *   <li>Action state (task completion, failure)</li>
 * </ul>
 *
 * @since 1.4.0
 */
public class DialogueTriggerChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(DialogueTriggerChecker.class);

    private final ForemanEntity minewright;
    private final CompanionMemory memory;

    // State tracking
    private Player lastGreetedPlayer = null;
    private long lastGreetingTime = 0;
    private boolean wasRaining = false;
    private long lastWeatherCheckTime = 0;

    // Cooldown tracking per trigger type
    private final Map<String, Long> triggerCooldowns = new HashMap<>();

    public DialogueTriggerChecker(ForemanEntity minewright, CompanionMemory memory) {
        this.minewright = minewright;
        this.memory = memory;
    }

    /**
     * Checks time-based triggers (morning, night, idle too long).
     */
    public TriggerCheckResult checkTimeBasedTriggers(int ticksSinceLastComment, int baseCooldownTicks) {
        Level level = minewright.level();
        long dayTime = level.getDayTime() % 24000;

        // Morning greeting (6:00 AM game time)
        if (dayTime >= 0 && dayTime < 2000) {
            if (canTrigger("morning", 24000)) {
                return new TriggerCheckResult(true, "morning", "It's morning!");
            }
        }

        // Night warning (8:00 PM game time)
        if (dayTime >= 18000 && dayTime < 20000) {
            if (canTrigger("night", 12000)) {
                return new TriggerCheckResult(true, "night", "It's getting dark!");
            }
        }

        // Idle comment (if idle for too long)
        if (!minewright.getActionExecutor().isExecuting() && ticksSinceLastComment > 1200) {
            if (canTrigger("idle_long", 6000)) {
                return new TriggerCheckResult(true, "idle_long", "Been idle a while");
            }
        }

        return TriggerCheckResult.NO_TRIGGER;
    }

    /**
     * Checks context-based triggers (biome, location, danger).
     */
    public TriggerCheckResult checkContextBasedTriggers() {
        if (minewright.level().isClientSide) {
            return TriggerCheckResult.NO_TRIGGER;
        }

        // Check biome
        try {
            String biome = minewright.level().getBiome(minewright.blockPosition()).unwrapKey()
                .map(key -> key.location().getPath())
                .orElse("unknown");

            // Special biomes
            if (biome.contains("nether")) {
                if (canTrigger("nether_biome", 3000)) {
                    return new TriggerCheckResult(true, "nether_biome", "Entered the Nether");
                }
            } else if (biome.contains("end")) {
                if (canTrigger("end_biome", 3000)) {
                    return new TriggerCheckResult(true, "end_biome", "Entered the End");
                }
            } else if (biome.contains("snow") || biome.contains("ice")) {
                if (canTrigger("cold_biome", 6000)) {
                    return new TriggerCheckResult(true, "cold_biome", "Entered cold biome");
                }
            } else if (biome.contains("desert")) {
                if (canTrigger("desert_biome", 6000)) {
                    return new TriggerCheckResult(true, "desert_biome", "Entered desert");
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Error checking biome for proactive dialogue", e);
        }

        // Check for danger (low health, nearby hostile)
        if (minewright.getHealth() < minewright.getMaxHealth() * 0.3) {
            if (canTrigger("low_health", 2000)) {
                return new TriggerCheckResult(true, "low_health", "Health is low!");
            }
        }

        return TriggerCheckResult.NO_TRIGGER;
    }

    /**
     * Checks weather-related triggers.
     */
    public TriggerCheckResult checkWeatherTriggers() {
        Level level = minewright.level();

        // Check weather occasionally
        long now = System.currentTimeMillis();
        if (now - lastWeatherCheckTime < 5000) {
            return TriggerCheckResult.NO_TRIGGER;
        }
        lastWeatherCheckTime = now;

        boolean isRaining = level.isRaining();
        boolean isThundering = level.isThundering();

        // Rain started
        if (isRaining && !wasRaining) {
            if (isThundering) {
                if (canTrigger("storm", 10000)) {
                    wasRaining = isRaining;
                    return new TriggerCheckResult(true, "storm", "Storm started!");
                }
            } else {
                if (canTrigger("raining", 15000)) {
                    wasRaining = isRaining;
                    return new TriggerCheckResult(true, "raining", "Rain started!");
                }
            }
        }

        wasRaining = isRaining;
        return TriggerCheckResult.NO_TRIGGER;
    }

    /**
     * Checks player proximity triggers (greeting, approach).
     */
    public TriggerCheckResult checkPlayerProximityTriggers() {
        Level level = minewright.level();
        if (level.isClientSide) {
            return TriggerCheckResult.NO_TRIGGER;
        }

        // Find nearest player
        net.minecraft.world.entity.player.Player nearestPlayer = null;
        double nearestDistance = Double.MAX_VALUE;

        for (net.minecraft.world.entity.player.Player player : level.players()) {
            double dist = minewright.position().distanceTo(player.position());
            if (dist < nearestDistance) {
                nearestDistance = dist;
                nearestPlayer = player;
            }
        }

        if (nearestPlayer == null) {
            return TriggerCheckResult.NO_TRIGGER;
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

                // Initialize relationship if needed
                if (memory.getPlayerName() == null) {
                    memory.initializeRelationship(nearestPlayer.getName().getString());
                }

                return new TriggerCheckResult(true, "player_approach", "Player approached");
            }
        }

        return TriggerCheckResult.NO_TRIGGER;
    }

    /**
     * Checks if a trigger can fire based on cooldown.
     */
    public boolean canTrigger(String triggerType, int cooldownTicks) {
        Long lastTrigger = triggerCooldowns.get(triggerType);
        if (lastTrigger == null) {
            return true;
        }

        long ticksSinceTrigger = (System.currentTimeMillis() - lastTrigger) / 50; // Convert ms to ticks
        return ticksSinceTrigger >= cooldownTicks;
    }

    /**
     * Records that a trigger has fired.
     */
    public void recordTrigger(String triggerType) {
        triggerCooldowns.put(triggerType, System.currentTimeMillis());
    }

    /**
     * Result of a trigger check.
     */
    public static class TriggerCheckResult {
        public static final TriggerCheckResult NO_TRIGGER = new TriggerCheckResult(false, null, null);

        public final boolean shouldTrigger;
        public final String triggerType;
        public final String context;

        public TriggerCheckResult(boolean shouldTrigger, String triggerType, String context) {
            this.shouldTrigger = shouldTrigger;
            this.triggerType = triggerType;
            this.context = context;
        }
    }
}
