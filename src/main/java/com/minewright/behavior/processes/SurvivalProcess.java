package com.minewright.behavior.processes;

import com.minewright.behavior.BehaviorProcess;
import com.minewright.entity.ForemanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
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
     * Threshold for falling velocity (negative Y indicates falling).
     */
    private static final double FALLING_VELOCITY_THRESHOLD = -0.5;

    /**
     * Search radius for detecting threats (blocks and entities).
     */
    private static final double THREAT_DETECTION_RADIUS = 16.0;

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
     *
     * <p>Current behavior:</p>
     * <ul>
     *   <li>Flee from nearby hostile mobs</li>
     *   <li>Find safe location</li>
     * </ul>
     *
     * <p>Future enhancements:</p>
     * <ul>
     *   <li>Eat food if available in inventory</li>
     *   <li>Call for help from other agents</li>
     *   <li>Use healing potions</li>
     * </ul>
     */
    private void handleLowHealth() {
        fleeFromNearestThreat();
    }

    /**
     * Handles fire/lava condition.
     *
     * <p>Current behavior:</p>
     * <ul>
     *   <li>Flee from dangerous position to safe ground</li>
     * </ul>
     *
     * <p>Future enhancements:</p>
     * <ul>
     *   <li>Use water bucket if available</li>
     *   <li>Break blocks to escape enclosed spaces</li>
     *   <li>Find water source to extinguish fire</li>
     * </ul>
     */
    private void handleFireLava() {
        fleeFromCurrentPosition();
    }

    /**
     * Handles drowning condition.
     *
     * <p>Current behavior:</p>
     * <ul>
     *   <li>Swim upward toward surface</li>
     * </ul>
     *
     * <p>Future enhancements:</p>
     * <ul>
     *   <li>Place torch or block to create air pocket</li>
     *   <li>Use water breathing potion if available</li>
     *   <li>Dig upward to surface if blocked</li>
     * </ul>
     */
    private void handleDrowning() {
        swimUpward();
    }

    /**
     * Handles falling condition.
     *
     * <p>Current behavior:</p>
     * <ul>
     *   <li>Informational only - falling happens too fast for reaction</li>
     * </ul>
     *
     * <p>Future enhancements:</p>
     * <ul>
     *   <li>Try to grab ledge on wall</li>
     *   <li>Use water bucket if available (MLG water bucket)</li>
     *   <li>Activate elytra if equipped</li>
     *   <li>Use slow fall potion</li>
     * </ul>
     */
    private void handleFalling() {
        // Falling happens too fast for reaction-based survival
        // This is primarily for detection and post-fall recovery
        LOGGER.debug("[{}] Falling detected at {} with velocity {}",
            foreman.getEntityName(), foreman.blockPosition(), String.format("%.2f", foreman.getDeltaMovement().y));
    }

    /**
     * Handles under attack condition.
     *
     * <p>Current behavior:</p>
     * <ul>
     *   <li>Flee from attacker</li>
     * </ul>
     *
     * <p>Future enhancements:</p>
     * <ul>
     *   <li>Fight back with equipped weapon if odds are good</li>
     *   <li>Use shield to block incoming damage</li>
     *   <li>Assess threat level - flee if outnumbered</li>
     *   <li>Call for help from nearby agents</li>
     * </ul>
     */
    private void handleAttack() {
        fleeFromNearestThreat();
    }

    // === Detection Methods ===

    private boolean isLowHealth() {
        return foreman.getHealth() / foreman.getMaxHealth() < CRITICAL_HEALTH_THRESHOLD;
    }

    private boolean isInLava() {
        if (foreman.level() == null) {
            return false;
        }
        BlockPos pos = foreman.blockPosition();
        return foreman.level().getBlockState(pos).is(Blocks.LAVA);
    }

    private boolean isOnFire() {
        return foreman.isOnFire() || foreman.getRemainingFireTicks() > 0;
    }

    private boolean isDrowning() {
        // Check if underwater and low on air
        int currentAir = foreman.getAirSupply();
        int maxAir = foreman.getMaxAirSupply();
        boolean underwater = foreman.isUnderWater();
        return underwater && currentAir < maxAir * 0.3; // Less than 30% air remaining
    }

    private boolean isFalling() {
        // Check if falling with significant downward velocity
        Vec3 deltaMovement = foreman.getDeltaMovement();
        boolean fallingFast = deltaMovement.y < FALLING_VELOCITY_THRESHOLD;

        // Also check if block below is air (indicating we're not on solid ground)
        boolean blockBelowIsAir = false;
        if (foreman.level() != null) {
            BlockPos pos = foreman.blockPosition();
            blockBelowIsAir = foreman.level().getBlockState(pos.below()).isAir();
        }

        return fallingFast && blockBelowIsAir;
    }

    private boolean isUnderAttack() {
        // Check if recently damaged by hostile mob
        LivingEntity attacker = foreman.getLastHurtByMob();
        if (attacker != null && attacker.isAlive()) {
            // Check if attacker is nearby and hostile
            double distance = foreman.position().distanceTo(attacker.position());
            return distance < THREAT_DETECTION_RADIUS;
        }
        return false;
    }

    // === Action Methods ===

    private void fleeFromNearestThreat() {
        LivingEntity attacker = foreman.getLastHurtByMob();
        if (attacker != null && attacker.isAlive()) {
            // Calculate flee direction (away from attacker)
            BlockPos fleePos = calculateFleePosition(attacker.blockPosition());
            if (fleePos != null) {
                navigateTo(fleePos);
                LOGGER.debug("[{}] Fleeing from attacker at {} to {}",
                    foreman.getEntityName(), attacker.blockPosition(), fleePos);
            }
        } else {
            // No specific threat - flee from current position to safety
            fleeFromCurrentPosition();
        }
    }

    private void fleeFromCurrentPosition() {
        BlockPos currentPos = foreman.blockPosition();

        // Find a safe position away from current danger
        BlockPos fleePos = findSafePosition(currentPos);

        if (fleePos != null) {
            navigateTo(fleePos);
            LOGGER.debug("[{}] Fleeing from danger at {} to {}",
                foreman.getEntityName(), currentPos, fleePos);
        }
    }

    private void swimUpward() {
        BlockPos currentPos = foreman.blockPosition();

        // Try to swim upward - find water's surface or air above
        BlockPos surfacePos = findWaterSurface(currentPos);

        if (surfacePos != null) {
            navigateTo(surfacePos);
            LOGGER.debug("[{}] Swimming upward from {} to surface at {}",
                foreman.getEntityName(), currentPos, surfacePos);
        } else {
            // No surface found - try moving up anyway
            BlockPos upPos = currentPos.above(5);
            navigateTo(upPos);
        }
    }

    /**
     * Calculates a flee position away from a threat.
     *
     * @param threatPos Position of the threat
     * @return Position to flee to, or null if none found
     */
    private BlockPos calculateFleePosition(BlockPos threatPos) {
        BlockPos currentPos = foreman.blockPosition();

        // Calculate direction away from threat
        int dx = currentPos.getX() - threatPos.getX();
        int dz = currentPos.getZ() - threatPos.getZ();

        // Normalize and scale to safe distance
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance < 0.1) {
            // On top of threat - pick random direction
            dx = 1;
            dz = 1;
        } else {
            dx = (int) (dx / distance * 10);
            dz = (int) (dz / distance * 10);
        }

        BlockPos fleePos = currentPos.offset(dx, 0, dz);

        // Ensure Y is safe (avoid falling)
        fleePos = new BlockPos(fleePos.getX(), currentPos.getY(), fleePos.getZ());

        return fleePos;
    }

    /**
     * Finds a safe position away from danger.
     *
     * @param dangerPos Current dangerous position
     * @return Safe position, or null if none found
     */
    private BlockPos findSafePosition(BlockPos dangerPos) {
        // Check nearby positions for safety
        int searchRadius = 8;

        for (int radius = 2; radius <= searchRadius; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    for (int dy = -2; dy <= 2; dy++) {
                        BlockPos candidate = dangerPos.offset(dx, dy, dz);

                        if (isPositionSafe(candidate)) {
                            return candidate;
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Checks if a position is safe (not lava, not fire, solid ground).
     *
     * @param pos Position to check
     * @return true if safe
     */
    private boolean isPositionSafe(BlockPos pos) {
        if (foreman.level() == null) {
            return false;
        }

        // Check for lava
        if (foreman.level().getBlockState(pos).is(Blocks.LAVA)) {
            return false;
        }

        // Check for fire
        if (foreman.level().getBlockState(pos).is(Blocks.FIRE)) {
            return false;
        }

        // Check for solid ground below
        BlockPos below = pos.below();
        if (!foreman.level().getBlockState(below).isSolidRender(foreman.level(), below)) {
            return false; // No solid ground
        }

        // Check for safe space at head level
        BlockPos head = pos.above();
        if (foreman.level().getBlockState(head).isSuffocating(foreman.level(), head)) {
            return false;
        }

        return true;
    }

    /**
     * Finds the water surface position.
     *
     * @param currentPos Current underwater position
     * @return Surface position, or null if not found
     */
    private BlockPos findWaterSurface(BlockPos currentPos) {
        if (foreman.level() == null) {
            return null;
        }

        // Search upward for air
        int maxSearch = 20; // Search up to 20 blocks up
        for (int y = currentPos.getY(); y < Math.min(currentPos.getY() + maxSearch, foreman.level().getMaxBuildHeight()); y++) {
            BlockPos checkPos = new BlockPos(currentPos.getX(), y, currentPos.getZ());

            if (!foreman.level().getBlockState(checkPos).is(Blocks.WATER)) {
                // Found air - surface is one block below
                return checkPos.below();
            }
        }

        return null;
    }

    /**
     * Navigates the entity to a target position.
     *
     * @param targetPos Target position
     */
    private void navigateTo(BlockPos targetPos) {
        if (foreman.getNavigation() != null) {
            // Use speed multiplier for urgency
            double speed = 1.5; // Faster than normal movement
            foreman.getNavigation().moveTo(targetPos.getX(), targetPos.getY(), targetPos.getZ(), speed);
        }
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
