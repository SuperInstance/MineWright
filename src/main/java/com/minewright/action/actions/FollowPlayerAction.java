package com.minewright.action.actions;

import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public class FollowPlayerAction extends BaseAction {
    private String playerName;
    private Player targetPlayer;
    private int ticksRunning;
    private static final int MAX_TICKS = 6000; // 5 minutes

    public FollowPlayerAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
    }

    @Override
    protected void onStart() {
        playerName = task.getStringParameter("player");
        ticksRunning = 0;
        
        findPlayer();
        
        if (targetPlayer == null) {
            result = ActionResult.failure("Player not found: " + playerName);
        }
    }

    @Override
    protected void onTick() {
        ticksRunning++;
        
        if (ticksRunning > MAX_TICKS) {
            result = ActionResult.success("Stopped following");
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
        foreman.getNavigation().stop();
    }

    @Override
    public String getDescription() {
        return "Follow player " + playerName;
    }

    private void findPlayer() {
        java.util.List<? extends Player> players = foreman.level().players();

        // First try exact name match
        for (Player player : players) {
            if (player.getName().getString().equalsIgnoreCase(playerName)) {
                targetPlayer = player;
                return;
            }
        }

        if (playerName != null && (playerName.contains("PLAYER") || playerName.contains("NAME") ||
            playerName.equalsIgnoreCase("me") || playerName.equalsIgnoreCase("you") || playerName.isEmpty())) {
            Player nearest = null;
            double nearestDistance = Double.MAX_VALUE;

            for (Player player : players) {
                double distance = foreman.distanceTo(player);
                if (distance < nearestDistance) {
                    nearest = player;
                    nearestDistance = distance;
                }
            }

            if (nearest != null) {
                targetPlayer = nearest;
                playerName = nearest.getName().getString(); // Update to actual name
                com.minewright.MineWrightMod.LOGGER.info("Foreman '{}' following nearest player: {}",
                    foreman.getSteveName(), playerName);
            }
        }
    }
}

