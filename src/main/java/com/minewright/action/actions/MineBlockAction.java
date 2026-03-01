package com.minewright.action.actions;

import com.minewright.testutil.TestLogger;
import org.slf4j.Logger;
import com.minewright.action.ActionResult;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import com.minewright.util.BlockNameMapper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Action that mines blocks in a directed tunnel pattern.
 *
 * <p><b>Features:</b></p>
 * <ul>
 *   <li><b>Directional Mining:</b> Mines in the player's look direction (straight tunnel)</li>
 *   <li><b>Ore Detection:</b> Scans ahead for target ore blocks</li>
 *   <li><b>Torch Placement:</b> Automatically places torches when light level is low</li>
 *   <li><b>Flying Mode:</b> Foreman flies for better access to ore veins</li>
 *   <li><b>Depth-Aware:</b> Knows optimal Y-levels for different ores</li>
 * </ul>
 *
 * <p><b>Parameters:</b></p>
 * <ul>
 *   <li><code>block</code> - Block/ore type to mine (e.g., iron_ore, diamond_ore)</li>
 *   <li><code>quantity</code> - Target quantity to mine (default 8)</li>
 * </ul>
 *
 * @since 1.0.0
 */
public class MineBlockAction extends BaseAction {
    private static final Logger LOGGER = TestLogger.getLogger(MineBlockAction.class);
    private Block targetBlock;
    private int targetQuantity;
    private int minedCount;
    private BlockPos currentTarget;
    private int searchRadius = 8; // Small search radius - stay near player
    private int ticksRunning;
    private int ticksSinceLastTorch = 0;
    private BlockPos miningStartPos; // Fixed mining spot in front of player
    private BlockPos currentTunnelPos; // Current position in the tunnel
    private int miningDirectionX = 0; // Direction to mine (-1, 0, or 1)
    private int miningDirectionZ = 0; // Direction to mine (-1, 0, or 1)
    private int ticksSinceLastMine = 0; // Delay between mining blocks
    private static final int MAX_TICKS = 24000; // 20 minutes for deep mining
    private static final int TORCH_INTERVAL = 100; // Place torch every 5 seconds (100 ticks)
    private static final int MIN_LIGHT_LEVEL = 8;
    private static final int MINING_DELAY = 10;
    private static final int MAX_MINING_RADIUS = 5;
    
    // Ore depth mappings for intelligent mining
    private static final Map<String, Integer> ORE_DEPTHS = new HashMap<>() {{
        put("iron_ore", 64);  // Iron spawns well at Y=64 and below
        put("deepslate_iron_ore", -16); // Deep iron
        put("coal_ore", 96);
        put("copper_ore", 48);
        put("gold_ore", 32);
        put("deepslate_gold_ore", -16);
        put("diamond_ore", -59);
        put("deepslate_diamond_ore", -59);
        put("redstone_ore", 16);
        put("deepslate_redstone_ore", -32);
        put("lapis_ore", 0);
        put("deepslate_lapis_ore", -16);
        put("emerald_ore", 256); // Mountain biomes
    }};

    public MineBlockAction(ForemanEntity foreman, Task task) {
        super(foreman, task);
    }

    @Override
    protected void onStart() {
        String blockName = task.getStringParameter("block");
        targetQuantity = task.getIntParameter("quantity", 8); // Mine reasonable amount by default
        minedCount = 0;
        ticksRunning = 0;
        ticksSinceLastTorch = 0;
        ticksSinceLastMine = 0;
        
        targetBlock = parseBlock(blockName);
        
        if (targetBlock == null || targetBlock == Blocks.AIR) {
            result = ActionResult.failure("Invalid block type: " + blockName);
            return;
        }
        
        net.minecraft.world.entity.player.Player nearestPlayer = findNearestPlayer();
        if (nearestPlayer != null) {
            net.minecraft.world.phys.Vec3 eyePos = nearestPlayer.getEyePosition(1.0F);
            net.minecraft.world.phys.Vec3 lookVec = nearestPlayer.getLookAngle();
            
            double angle = Math.atan2(lookVec.z, lookVec.x) * 180.0 / Math.PI;
            angle = (angle + 360) % 360;
            
            if (angle >= 315 || angle < 45) {
                miningDirectionX = 1; miningDirectionZ = 0; // East (+X)
            } else if (angle >= 45 && angle < 135) {
                miningDirectionX = 0; miningDirectionZ = 1; // South (+Z)
            } else if (angle >= 135 && angle < 225) {
                miningDirectionX = -1; miningDirectionZ = 0; // West (-X)
            } else {
                miningDirectionX = 0; miningDirectionZ = -1; // North (-Z)
            }
            
            net.minecraft.world.phys.Vec3 targetPos = eyePos.add(lookVec.scale(3));
            
            BlockPos lookTarget = new BlockPos(
                (int)Math.floor(targetPos.x),
                (int)Math.floor(targetPos.y),
                (int)Math.floor(targetPos.z)
            );
            
            miningStartPos = lookTarget;
            for (int y = lookTarget.getY(); y > lookTarget.getY() - 20 && y > -64; y--) {
                BlockPos groundCheck = new BlockPos(lookTarget.getX(), y, lookTarget.getZ());
                if (foreman.level().getBlockState(groundCheck).isSolidRender(foreman.level(), groundCheck)) {
                    miningStartPos = groundCheck.above(); // Stand on top of solid block
                    break;
                }
            }
            
            currentTunnelPos = miningStartPos;
            foreman.teleportTo(miningStartPos.getX() + 0.5, miningStartPos.getY(), miningStartPos.getZ() + 0.5);
            
            String[] dirNames = {"North", "East", "South", "West"};
            int dirIndex = miningDirectionZ == -1 ? 0 : (miningDirectionX == 1 ? 1 : (miningDirectionZ == 1 ? 2 : 3));
            LOGGER.info("Foreman '{}' mining {} in ONE direction: {}",
                foreman.getEntityName(), targetBlock.getName().getString(), dirNames[dirIndex]);
        } else {
            miningStartPos = foreman.blockPosition();
            currentTunnelPos = miningStartPos;
            miningDirectionX = 1; // Default to East
            miningDirectionZ = 0;
        }
        
        foreman.setFlying(true);
        
        equipIronPickaxe();
        
        LOGGER.info("Foreman '{}' mining {} - staying at {} [SLOW & VISIBLE]",
            foreman.getEntityName(), targetBlock.getName().getString(), miningStartPos);
        
        // Look for ore nearby
        findNextBlock();
    }

    @Override
    protected void onTick() {
        ticksRunning++;
        ticksSinceLastTorch++;
        ticksSinceLastMine++;
        
        if (ticksRunning > MAX_TICKS) {
            foreman.setFlying(false);
            foreman.setItemInHand(InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
            result = ActionResult.failure("Mining timeout - only found " + minedCount + " blocks");
            return;
        }
        
        if (ticksSinceLastTorch >= TORCH_INTERVAL) {
            placeTorchIfDark();
            ticksSinceLastTorch = 0;
        }
        
        if (ticksSinceLastMine < MINING_DELAY) {
            return; // Still waiting
        }
        
        if (currentTarget == null) {
            findNextBlock();
            
            if (currentTarget == null) {
                if (minedCount >= targetQuantity) {
                    // Found enough ore, mission accomplished
                    foreman.setFlying(false);
                    foreman.setItemInHand(InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
                    result = ActionResult.success("Mined " + minedCount + " " + targetBlock.getName().getString());
                    return;
                } else {
                    mineNearbyBlock();
                    return;
                }
            }
        }
        
        if (foreman.level().getBlockState(currentTarget).getBlock() == targetBlock) {
            foreman.teleportTo(currentTarget.getX() + 0.5, currentTarget.getY(), currentTarget.getZ() + 0.5);
            
            foreman.swing(InteractionHand.MAIN_HAND, true);
            
            foreman.level().destroyBlock(currentTarget, true);
            minedCount++;
            ticksSinceLastMine = 0; // Reset delay timer
            
            LOGGER.info("Foreman '{}' moved to ore and mined {} at {} - Total: {}/{}",
                foreman.getEntityName(), targetBlock.getName().getString(), currentTarget,
                minedCount, targetQuantity);
            
            if (minedCount >= targetQuantity) {
                foreman.setFlying(false);
                foreman.setItemInHand(InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
                result = ActionResult.success("Mined " + minedCount + " " + targetBlock.getName().getString());
                return;
            }
            
            currentTarget = null;
        } else {
            currentTarget = null;
        }
    }

    @Override
    protected void onCancel() {
        foreman.setFlying(false);
        foreman.getNavigation().stop();
        foreman.setItemInHand(InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
    }

    @Override
    public String getDescription() {
        return "Mine " + targetQuantity + " " + targetBlock.getName().getString() + " (" + minedCount + " found)";
    }

    /**
     * Check light level and place torch if too dark
     */
    private void placeTorchIfDark() {
        BlockPos stevePos = foreman.blockPosition();
        int lightLevel = foreman.level().getBrightness(net.minecraft.world.level.LightLayer.BLOCK, stevePos);
        
        if (lightLevel < MIN_LIGHT_LEVEL) {
            BlockPos torchPos = findTorchPosition(stevePos);
            
            if (torchPos != null && foreman.level().getBlockState(torchPos).isAir()) {
                foreman.level().setBlock(torchPos, Blocks.TORCH.defaultBlockState(), 3);
                LOGGER.info("Foreman '{}' placed torch at {} (light level was {})",
                    foreman.getEntityName(), torchPos, lightLevel);
                
                foreman.swing(InteractionHand.MAIN_HAND, true);
            }
        }
    }
    
    /**
     * Find a good position to place a torch (on floor or wall)
     */
    private BlockPos findTorchPosition(BlockPos center) {
        BlockPos floorPos = center.below();
        if (foreman.level().getBlockState(floorPos).isSolidRender(foreman.level(), floorPos) &&
            foreman.level().getBlockState(center).isAir()) {
            return center;
        }

        BlockPos[] wallPositions = {
            center.north(), center.south(), center.east(), center.west()
        };

        for (BlockPos wallPos : wallPositions) {
            if (foreman.level().getBlockState(wallPos).isSolidRender(foreman.level(), wallPos) &&
                foreman.level().getBlockState(center).isAir()) {
                return center;
            }
        }

        return null;
    }

    /**
     * Mine forward in ONE DIRECTION - creates a straight tunnel!
     * Steve progresses forward block by block
     */
    private void mineNearbyBlock() {
        BlockPos centerPos = currentTunnelPos;
        BlockPos abovePos = centerPos.above();
        BlockPos belowPos = centerPos.below();
        
        BlockState centerState = foreman.level().getBlockState(centerPos);
        if (!centerState.isAir() && centerState.getBlock() != Blocks.BEDROCK) {
            foreman.teleportTo(centerPos.getX() + 0.5, centerPos.getY(), centerPos.getZ() + 0.5);
            foreman.swing(InteractionHand.MAIN_HAND, true);
            foreman.level().destroyBlock(centerPos, true);
            LOGGER.info("Foreman '{}' mining tunnel at {}", foreman.getEntityName(), centerPos);
        }
        
        BlockState aboveState = foreman.level().getBlockState(abovePos);
        if (!aboveState.isAir() && aboveState.getBlock() != Blocks.BEDROCK) {
            foreman.swing(InteractionHand.MAIN_HAND, true);
            foreman.level().destroyBlock(abovePos, true);
        }
        
        BlockState belowState = foreman.level().getBlockState(belowPos);
        if (!belowState.isAir() && belowState.getBlock() != Blocks.BEDROCK) {
            foreman.swing(InteractionHand.MAIN_HAND, true);
            foreman.level().destroyBlock(belowPos, true);
        }
        
        currentTunnelPos = currentTunnelPos.offset(miningDirectionX, 0, miningDirectionZ);
        
        ticksSinceLastMine = 0; // Reset delay
    }

    /**
     * Find ore blocks in the tunnel ahead
     * Searches forward in the mining direction
     */
    private void findNextBlock() {
        List<BlockPos> foundBlocks = new ArrayList<>();
        
        for (int distance = 0; distance < 20; distance++) {
            BlockPos checkPos = currentTunnelPos.offset(miningDirectionX * distance, 0, miningDirectionZ * distance);
            
            for (int y = -1; y <= 1; y++) {
                BlockPos orePos = checkPos.offset(0, y, 0);
                if (foreman.level().getBlockState(orePos).getBlock() == targetBlock) {
                    foundBlocks.add(orePos);
                }
            }
        }
        
        if (!foundBlocks.isEmpty()) {
            currentTarget = foundBlocks.stream()
                .min((a, b) -> Double.compare(a.distSqr(currentTunnelPos), b.distSqr(currentTunnelPos)))
                .orElse(null);
            
            if (currentTarget != null) {
                LOGGER.info("Foreman '{}' found {} ahead in tunnel at {}",
                    foreman.getEntityName(), targetBlock.getName().getString(), currentTarget);
            }
        }
    }

    /**
     * Equip an iron pickaxe for mining
     */
    private void equipIronPickaxe() {
        // Give Foreman an iron pickaxe if he doesn't have one
        net.minecraft.world.item.ItemStack pickaxe = new net.minecraft.world.item.ItemStack(
            net.minecraft.world.item.Items.IRON_PICKAXE
        );
        foreman.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, pickaxe);
        LOGGER.info("Foreman '{}' equipped iron pickaxe for mining", foreman.getEntityName());
    }

    /**
     * Find the nearest player to determine mining direction
     */
    private net.minecraft.world.entity.player.Player findNearestPlayer() {
        java.util.List<? extends net.minecraft.world.entity.player.Player> players = foreman.level().players();
        
        if (players.isEmpty()) {
            return null;
        }
        
        net.minecraft.world.entity.player.Player nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        
        for (net.minecraft.world.entity.player.Player player : players) {
            if (!player.isAlive() || player.isRemoved() || player.isSpectator()) {
                continue;
            }
            
            double distance = foreman.distanceTo(player);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = player;
            }
        }
        
        return nearest;
    }

    private Block parseBlock(String blockName) {
        // Use BlockNameMapper to normalize common aliases to valid registry names
        String normalizedBlockName = BlockNameMapper.normalize(blockName);

        // Add minecraft namespace if not present
        if (!normalizedBlockName.contains(":")) {
            normalizedBlockName = "minecraft:" + normalizedBlockName;
        }

        ResourceLocation resourceLocation = new ResourceLocation(normalizedBlockName);
        return ForgeRegistries.BLOCKS.getValue(resourceLocation);
    }
}

