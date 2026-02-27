package com.minewright.action.actions;

import com.minewright.MineWrightMod;
import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.List;

/**
 * Idle behavior for Foreman - follows the nearest player when not working.
 * This action runs continuously until a task is given.
 * Teleports to player if too far away.
 */
public class IdleFollowAction extends BaseAction {
    private Player targetPlayer;
    private int ticksSincePlayerSearch;
    private static final int PLAYER_SEARCH_INTERVAL = 100; // Search for new player every 5 seconds
    private static final double FOLLOW_DISTANCE = 4.0; // Stay this far from player
    private static final double MIN_DISTANCE = 2.5; // Stop moving if closer than this
    private static final double TELEPORT_DISTANCE = 50.0; // Teleport if further than 50 blocks

    public IdleFollowAction(ForemanEntity foreman) {
        super(foreman, new Task("idle_follow", new HashMap<>()));
    }

    @Override
    protected void onStart() {
        if (foreman == null || foreman.level() == null || foreman.getNavigation() == null) {
            MineWrightMod.LOGGER.warn("Foreman, level, or navigation not available for idle follow");
            return;
        }

        ticksSincePlayerSearch = 0;
        findNearestPlayer();

        if (targetPlayer == null) {
            MineWrightMod.LOGGER.debug("Foreman '{}' has no player to follow (idle)", foreman.getEntityName());
        }
    }

    @Override
    protected void onTick() {
        if (foreman == null || foreman.level() == null || foreman.getNavigation() == null) {
            return;
        }

        ticksSincePlayerSearch++;

        // Periodically search for a better/closer player
        if (ticksSincePlayerSearch >= PLAYER_SEARCH_INTERVAL) {
            findNearestPlayer();
            ticksSincePlayerSearch = 0;
        }

        if (targetPlayer == null || !targetPlayer.isAlive() || targetPlayer.isRemoved()) {
            findNearestPlayer();
            if (targetPlayer == null) {
                // No players around, just stand idle
                foreman.getNavigation().stop();
                return;
            }
        }

        // Follow the player at a comfortable distance
        double distance = foreman.distanceTo(targetPlayer);
        if (distance > TELEPORT_DISTANCE) {
            // Teleport near the player (3-5 blocks away)
            double offsetX = (Math.random() - 0.5) * 6; // Random offset between -3 and +3
            double offsetZ = (Math.random() - 0.5) * 6;

            double targetX = targetPlayer.getX() + offsetX;
            double targetY = targetPlayer.getY();
            double targetZ = targetPlayer.getZ() + offsetZ;

            net.minecraft.core.BlockPos checkPos = new net.minecraft.core.BlockPos((int)targetX, (int)targetY, (int)targetZ);
            for (int i = 0; i < 10; i++) {
                net.minecraft.core.BlockPos groundPos = checkPos.below(i);
                if (!foreman.level().getBlockState(groundPos).isAir() &&
                    foreman.level().getBlockState(groundPos.above()).isAir()) {
                    // Found solid ground with air above
                    targetY = groundPos.above().getY();
                    break;
                }
            }

            foreman.teleportTo(targetX, targetY, targetZ);
            foreman.getNavigation().stop(); // Clear navigation after teleport

            MineWrightMod.LOGGER.info("Foreman '{}' teleported to player (was {} blocks away)",
                foreman.getEntityName(), (int)distance);

        } else if (distance > FOLLOW_DISTANCE) {
            // Too far, move closer (normal walking)
            foreman.getNavigation().moveTo(targetPlayer, 1.0);
        } else if (distance < MIN_DISTANCE) {
            // Too close, stop
            foreman.getNavigation().stop();
        } else {
            if (!foreman.getNavigation().isDone()) {
                foreman.getNavigation().stop();
            }
        }

        // This action never completes on its own - it runs until cancelled
    }

    @Override
    protected void onCancel() {
        if (foreman != null && foreman.getNavigation() != null) {
            foreman.getNavigation().stop();
        }
    }

    @Override
    public String getDescription() {
        return "Following player (idle)";
    }

    /**
     * Find the nearest player to follow
     */
    private void findNearestPlayer() {
        if (foreman == null || foreman.level() == null) {
            return;
        }

        List<? extends Player> players = foreman.level().players();
        if (players == null || players.isEmpty()) {
            targetPlayer = null;
            return;
        }

        Player nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Player player : players) {
            if (!player.isAlive() || player.isRemoved() || player.isSpectator()) {
                continue;
            }

            double distance = foreman.distanceTo(player);
            if (distance < nearestDistance) {
                nearest = player;
                nearestDistance = distance;
            }
        }

        if (nearest != targetPlayer && nearest != null) {
            MineWrightMod.LOGGER.debug("Foreman '{}' now following {} (idle)",
                foreman.getEntityName(), nearest.getName().getString());
        }

        targetPlayer = nearest;
    }
}
