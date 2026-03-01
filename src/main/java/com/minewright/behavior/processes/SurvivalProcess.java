package com.minewright.behavior.processes;

import com.minewright.behavior.BehaviorProcess;
import com.minewright.entity.ForemanEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * High-priority survival process that interrupts all other behaviors when the agent is in danger.
 *
 * <p>This process has the highest priority (100) and will preempt all other behaviors when
 * survival conditions are met. This is critical for keeping the agent alive and preventing
 * death or item loss.</p>
 *
 * <p><b>Survival Conditions (canRun):</b></p>
 * <ul>
 *   <li>Health below 30% (critical damage)</li>
 *   <li>On fire or in lava</li>
 *   <li>Drowning (underwater with low air)</li>
 *   <li>Falling from great height</li>
 *   <li>Under attack by hostile mob</li>
 * </ul>
 *
 * <p><b>Survival Actions (tick):</b></p>
 * <ul>
 *   <li>Flee from danger (run away from threat)</li>
 *   <li>Eat food to restore health</li>
 *   <li>Escape lava/fire (find water or safe ground)</li>
 *   <li>Surface for air (if drowning)</li>
 *   <li>Equip armor or weapon for defense</li>
 * </ul>
 *
 * @see BehaviorProcess
 * @since 1.2.0
 */
public class SurvivalProcess implements BehaviorProcess {

    private static final Logger LOGGER = LoggerFactory.getLogger(SurvivalProcess.class);

    /**
     * Priority level for survival (highest - interrupts everything).
     */
    private static final int PRIORITY = 100;

    /**
     * Health threshold for critical condition.
     */
    private static final float CRITICAL_HEALTH_THRESHOLD = 0.3f; // 30%

    /**
     * Air threshold for drowning condition.
     */
    private static final int CRITICAL_AIR_THRESHOLD = 10; // ticks of air remaining

    /**
     * The foreman entity this process is managing.
     */
    private final ForemanEntity foreman;

    /**
     * Whether this process is currently active.
     */
    private boolean active = false;

    /**
     * The type of survival threat detected.
     */
    private SurvivalThreat threatType = SurvivalThreat.NONE;

    /**
     * Ticks since process activation.
     */
    private int ticksActive = 0;

    /**
     * Types of survival threats the agent can face.
     */
    private enum SurvivalThreat {
        NONE("No threat"),
        LOW_HEALTH("Critical health"),
        ON_FIRE("On fire"),
        IN_LAVA("In lava"),
        DROWNING("Drowning"),
        FALLING("Falling"),
        UNDER_ATTACK("Under attack");

        private final String description;

        SurvivalThreat(String description) {
            this.description = description;
        }

        String getDescription() {
            return description;
        }
    }

    /**
     * Creates a new SurvivalProcess for the given foreman.
     *
     * @param foreman The foreman entity to manage survival for
     */
    public SurvivalProcess(ForemanEntity foreman) {
        this.foreman = foreman;
    }

    @Override
    public String getName() {
        return "Survival";
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public boolean canRun() {
        if (foreman == null || !foreman.isAlive()) {
            return false;
        }

        // Check all survival conditions
        threatType = detectThreat();

        return threatType != SurvivalThreat.NONE;
    }

    @Override
    public void tick() {
        ticksActive++;

        // Log survival status every 20 ticks (1 second)
        if (ticksActive % 20 == 0) {
            LOGGER.info("[{}] Survival mode active: {} (ticks: {})",
                foreman.getEntityName(), threatType.getDescription(), ticksActive);
        }

        // Execute survival behavior based on threat type
        switch (threatType) {
            case LOW_HEALTH -> handleLowHealth();
            case ON_FIRE, IN_LAVA -> handleFireLava();
            case DROWNING -> handleDrowning();
            case FALLING -> handleFalling();
            case UNDER_ATTACK -> handleAttack();
            case NONE -> {
                // Should not happen - canRun() should prevent this
                LOGGER.warn("[{}] SurvivalProcess tick() called but no threat detected",
                    foreman.getEntityName());
            }
        }
    }

    @Override
    public void onActivate() {
        active = true;
        ticksActive = 0;

        LOGGER.warn("[{}] SURVIVAL MODE ACTIVATED: {}",
            foreman.getEntityName(), threatType.getDescription());

        // Send alert to player
        foreman.sendChatMessage("Help! " + getThreatMessage(threatType));
    }

    @Override
    public void onDeactivate() {
        active = false;

        LOGGER.info("[{}] Survival mode deactivated (was active for {} ticks)",
            foreman.getEntityName(), ticksActive);

        // Notify player that danger has passed
        if (ticksActive > 20) { // Only if active for more than 1 second
            foreman.sendChatMessage("Phew! That was close!");
        }
    }

    /**
     * Detects the most urgent survival threat.
     *
     * @return The detected threat type, or NONE if safe
     */
    private SurvivalThreat detectThreat() {
        // Check most urgent threats first

        // IN_LAVA - highest priority (instant damage + item loss)
        if (isInLava()) {
            return SurvivalThreat.IN_LAVA;
        }

        // ON_FIRE - second highest (ongoing damage)
        if (isOnFire()) {
            return SurvivalThreat.ON_FIRE;
        }

        // DROWNING - third priority (imminent death)
        if (isDrowning()) {
            return SurvivalThreat.DROWNING;
        }

        // FALLING - fourth priority (fall damage)
        if (isFalling()) {
            return SurvivalThreat.FALLING;
        }

        // LOW_HEALTH - fifth priority (already hurt, need to eat)
        if (isLowHealth()) {
            return SurvivalThreat.LOW_HEALTH;
        }

        // UNDER_ATTACK - lowest priority (can fight back)
        if (isUnderAttack()) {
            return SurvivalThreat.UNDER_ATTACK;
        }

        return SurvivalThreat.NONE;
    }

    /**
     * Handles low health condition.
     */
    private void handleLowHealth() {
        // TODO: Eat food if available
        // TODO: Flee to safe location
        // TODO: Call for help

        // For now, just flee from nearby hostile mobs
        fleeFromNearestThreat();
    }

    /**
     * Handles fire/lava condition.
     */
    private void handleFireLava() {
        // TODO: Find water or safe ground
        // TODO: Use water bucket if available
        // TODO: Break blocks to escape

        // For now, just try to move away from current position
        fleeFromCurrentPosition();
    }

    /**
     * Handles drowning condition.
     */
    private void handleDrowning() {
        // TODO: Swim upward to surface
        // TODO: Place torch or block for air pocket
        // TODO: Use water breathing potion if available

        // For now, try to move upward
        swimUpward();
    }

    /**
     * Handles falling condition.
     */
    private void handleFalling() {
        // TODO: Try to grab ledge
        // TODO: Use water bucket if available
        // TODO: Activate elytra if available

        // For now, try to slow fall (if we have mechanics for it)
        // This is mostly informational - falling happens fast
    }

    /**
     * Handles under attack condition.
     */
    private void handleAttack() {
        // TODO: Fight back with equipped weapon
        // TODO: Use shield to block
        // TODO: Flee if outnumbered or outmatched

        // For now, just flee
        fleeFromNearestThreat();
    }

    // === Detection Methods ===

    private boolean isLowHealth() {
        return foreman.getHealth() / foreman.getMaxHealth() < CRITICAL_HEALTH_THRESHOLD;
    }

    private boolean isInLava() {
        // Check if entity is in lava block
        // TODO: Implement actual lava detection
        return false; // Placeholder
    }

    private boolean isOnFire() {
        // Check if entity is on fire
        // TODO: Implement actual fire detection
        return false; // Placeholder
    }

    private boolean isDrowning() {
        // Check if underwater and low on air
        // TODO: Implement actual drowning detection
        return false; // Placeholder
    }

    private boolean isFalling() {
        // Check if falling with significant downward velocity
        // TODO: Implement actual falling detection
        return false; // Placeholder
    }

    private boolean isUnderAttack() {
        // Check if recently damaged by hostile mob
        // TODO: Implement actual attack detection
        return false; // Placeholder
    }

    // === Action Methods ===

    private void fleeFromNearestThreat() {
        // TODO: Implement fleeing logic
        // Find nearest threat, calculate opposite direction, move
    }

    private void fleeFromCurrentPosition() {
        // TODO: Implement fleeing logic
        // Move away from current position (e.g., away from lava)
    }

    private void swimUpward() {
        // TODO: Implement swimming logic
        // Move upward toward surface
    }

    private String getThreatMessage(SurvivalThreat threat) {
        return switch (threat) {
            case LOW_HEALTH -> "I'm badly hurt!";
            case ON_FIRE -> "I'm on fire!";
            case IN_LAVA -> "I'm in lava! Help!";
            case DROWNING -> "I can't breathe!";
            case FALLING -> "I'm falling!";
            case UNDER_ATTACK -> "I'm under attack!";
            case NONE -> "Something's wrong!";
        };
    }
}
