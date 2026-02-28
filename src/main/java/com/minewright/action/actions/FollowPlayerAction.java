package com.minewright.action.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import com.minewright.testutil.TestLogger;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

import java.util.List;

public class FollowPlayerAction extends BaseAction {
    private static final Logger LOGGER = TestLogger.getLogger(FollowPlayerAction.class);
    private String playerName;
    private Player targetPlayer;
    private int ticksRunning;
    private static final int MAX_TICKS = 6000; // 5 minutes

    public FollowPlayerAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
    }

    @Override
    protected void onStart() {
        if (foreman == null || foreman.level() == null || foreman.getNavigation() == null) {
            result = ActionResult.failure("Foreman, level, or navigation not available");
            return;
        }

        playerName = task.getStringParameter("player");
        ticksRunning = 0;

        findPlayer();

        if (targetPlayer == null) {
            result = ActionResult.failure("Player not found: " + (playerName != null ? playerName : "unspecified"));
        }
    }

    @Override
    protected void onTick() {
        if (foreman == null || foreman.getNavigation() == null) {
            result = ActionResult.failure("Invalid state during following");
            return;
        }

        ticksRunning++;

        if (ticksRunning > MAX_TICKS) {
            result = ActionResult.success("Stopped following (timeout)");
            return;
        }

        if (targetPlayer == null || !targetPlayer.isAlive() || targetPlayer.isRemoved()) {
            findPlayer();
            if (targetPlayer == null) {
                result = ActionResult.failure("Lost track of player");
                return;
            }
        }

        double distance = foreman.distanceTo(targetPlayer);
        if (distance > 3.0) {
            foreman.getNavigation().moveTo(targetPlayer, 1.0);
        } else if (distance < 2.0) {
            foreman.getNavigation().stop();
        }
    }

    @Override
    protected void onCancel() {
        if (foreman != null && foreman.getNavigation() != null) {
            foreman.getNavigation().stop();
        }
    }

    @Override
    public String getDescription() {
        return "Follow player " + (playerName != null ? playerName : "unknown");
    }

    private void findPlayer() {
        if (foreman == null || foreman.level() == null) {
            return;
        }

        java.util.List<? extends Player> players = foreman.level().players();
        if (players == null || players.isEmpty()) {
            return;
        }

        // First try exact name match
        if (playerName != null && !playerName.isEmpty()) {
            for (Player player : players) {
                if (player.getName().getString().equalsIgnoreCase(playerName)) {
                    targetPlayer = player;
                    return;
                }
            }
        }

        // Fall back to nearest player if using generic names
        if (playerName != null && (playerName.contains("PLAYER") || playerName.contains("NAME") ||
            playerName.equalsIgnoreCase("me") || playerName.equalsIgnoreCase("you") || playerName.isEmpty())) {
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

            if (nearest != null) {
                targetPlayer = nearest;
                playerName = nearest.getName().getString(); // Update to actual name
                LOGGER.info("Foreman '{}' following nearest player: {}",
                    foreman.getEntityName(), playerName);
            }
        }
    }
}

