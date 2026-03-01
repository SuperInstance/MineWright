package com.minewright.action.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import com.minewright.testutil.TestLogger;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;

import java.util.List;

/**
 * Action that engages in combat against hostile entities.
 *
 * <p><b>Features:</b></p>
 * <ul>
 *   <li><b>Target Detection:</b> Automatically finds and locks onto hostile mobs</li>
 *   <li><b>Sprinting:</b> Moves faster when chasing targets</li>
 *   <li><b>Invulnerability:</b> Foreman is invulnerable during combat to prevent death</li>
 *   <li><b>Stuck Detection:</b> Teleports closer if movement is blocked</li>
 * </ul>
 *
 * <p><b>Parameters:</b></p>
 * <ul>
 *   <li><code>target</code> - Entity type to attack (default: hostile mobs)</li>
 * </ul>
 *
 * @since 1.0.0
 */
public class CombatAction extends BaseAction {
    private static final Logger LOGGER = TestLogger.getLogger(CombatAction.class);
    private String targetType;
    private LivingEntity target;
    private int ticksRunning;
    private int ticksStuck;
    private double lastX, lastZ;
    private static final int MAX_TICKS = 600;
    private static final double ATTACK_RANGE = 3.5;

    public CombatAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
    }

    @Override
    protected void onStart() {
        if (foreman == null || foreman.getNavigation() == null || foreman.level() == null) {
            result = ActionResult.failure("Foreman, navigation, or level not available");
            return;
        }

        targetType = task.getStringParameter("target");
        if (targetType == null || targetType.isEmpty()) {
            targetType = "mob"; // Default to hostile mobs
        }

        ticksRunning = 0;
        ticksStuck = 0;

        // Make sure we're not flying (in case we were building)
        foreman.setFlying(false);
        foreman.setInvulnerableBuilding(true);

        findTarget();

        if (target == null) {
            LOGGER.warn("Foreman '{}' no targets nearby", foreman.getEntityName());
        }
    }

    @Override
    protected void onTick() {
        if (foreman == null || foreman.getNavigation() == null) {
            result = ActionResult.failure("Invalid state during combat");
            return;
        }

        ticksRunning++;

        if (ticksRunning > MAX_TICKS) {
            // Combat complete - clean up and disable invulnerability
            foreman.setInvulnerableBuilding(false);
            foreman.setSprinting(false);
            foreman.getNavigation().stop();
            LOGGER.info("Foreman '{}' combat complete, invulnerability disabled",
                foreman.getEntityName());
            result = ActionResult.success("Combat complete");
            return;
        }

        // Re-search for targets periodically or if current target is invalid
        if (target == null || !target.isAlive() || target.isRemoved()) {
            if (ticksRunning % 20 == 0) {
                findTarget();
            }
            if (target == null) {
                return; // Keep searching
            }
        }

        double distance = foreman.distanceTo(target);

        foreman.setSprinting(true);
        foreman.getNavigation().moveTo(target, 2.5); // High speed multiplier for sprinting

        double currentX = foreman.getX();
        double currentZ = foreman.getZ();
        if (Math.abs(currentX - lastX) < 0.1 && Math.abs(currentZ - lastZ) < 0.1) {
            ticksStuck++;

            if (ticksStuck > 40 && distance > ATTACK_RANGE) {
                // Teleport 4 blocks closer to target
                double dx = target.getX() - foreman.getX();
                double dz = target.getZ() - foreman.getZ();
                double dist = Math.sqrt(dx*dx + dz*dz);
                if (dist > 0) {
                    double moveAmount = Math.min(4.0, dist - ATTACK_RANGE);

                    foreman.teleportTo(
                        foreman.getX() + (dx/dist) * moveAmount,
                        foreman.getY(),
                        foreman.getZ() + (dz/dist) * moveAmount
                    );
                    ticksStuck = 0;
                    LOGGER.info("Foreman '{}' was stuck, teleported closer to target",
                        foreman.getEntityName());
                }
            }
        } else {
            ticksStuck = 0;
        }
        lastX = currentX;
        lastZ = currentZ;

        if (distance <= ATTACK_RANGE) {
            foreman.doHurtTarget(target);
            foreman.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);

            // Attack 3 times per second (every 6-7 ticks)
            if (ticksRunning % 7 == 0) {
                foreman.doHurtTarget(target);
            }
        }
    }

    @Override
    protected void onCancel() {
        if (foreman != null) {
            foreman.setInvulnerableBuilding(false);
            if (foreman.getNavigation() != null) {
                foreman.getNavigation().stop();
            }
            foreman.setSprinting(false);
            foreman.setFlying(false);
        }
        target = null;
        LOGGER.info("Foreman '{}' combat cancelled, invulnerability disabled",
            foreman != null ? foreman.getEntityName() : "unknown");
    }

    @Override
    public String getDescription() {
        return "Attack " + (targetType != null ? targetType : "hostile mobs");
    }

    private void findTarget() {
        if (foreman == null || foreman.level() == null) {
            return;
        }

        AABB searchBox = foreman.getBoundingBox().inflate(32.0);
        List<Entity> entities = foreman.level().getEntities(foreman, searchBox);

        if (entities == null || entities.isEmpty()) {
            return;
        }

        LivingEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Entity entity : entities) {
            if (entity instanceof LivingEntity living && isValidTarget(living)) {
                double distance = foreman.distanceTo(living);
                if (distance < nearestDistance) {
                    nearest = living;
                    nearestDistance = distance;
                }
            }
        }

        target = nearest;
        if (target != null) {
            LOGGER.info("Foreman '{}' locked onto: {} at {}m",
                foreman.getEntityName(), target.getType().toString(), (int)nearestDistance);
        }
    }

    private boolean isValidTarget(LivingEntity entity) {
        if (!entity.isAlive() || entity.isRemoved()) {
            return false;
        }

        // Don't attack other Foremen or players
        if (entity instanceof ForemanEntity || entity instanceof net.minecraft.world.entity.player.Player) {
            return false;
        }

        String targetLower = targetType.toLowerCase();

        // Match ANY hostile mob
        if (targetLower.contains("mob") || targetLower.contains("hostile") ||
            targetLower.contains("monster") || targetLower.equals("any")) {
            return entity instanceof Monster;
        }

        // Match specific entity type
        String entityTypeName = entity.getType().toString().toLowerCase();
        return entityTypeName.contains(targetLower);
    }
}
