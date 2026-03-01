package com.minewright.script;

import com.minewright.entity.ForemanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

/**
 * Context information for script generation.
 *
 * <p>Provides the LLM with relevant information about the agent's current state,
 * environment, inventory, and nearby blocks to generate context-aware scripts.</p>
 *
 * @see ScriptGenerator
 * @since 1.3.0
 */
public class ScriptGenerationContext {

    private final String agentId;
    private final String agentName;
    private final BlockPos agentPosition;
    private final Map<String, Object> agentState;
    private final Map<String, Integer> inventory;
    private final List<NearbyBlock> nearbyBlocks;
    private final String biome;
    private final String timeOfDay;
    private final Map<String, Object> customContext;

    private ScriptGenerationContext(Builder builder) {
        this.agentId = builder.agentId;
        this.agentName = builder.agentName;
        this.agentPosition = builder.agentPosition;
        this.agentState = Collections.unmodifiableMap(new HashMap<>(builder.agentState));
        this.inventory = Collections.unmodifiableMap(new HashMap<>(builder.inventory));
        this.nearbyBlocks = Collections.unmodifiableList(new ArrayList<>(builder.nearbyBlocks));
        this.biome = builder.biome;
        this.timeOfDay = builder.timeOfDay;
        this.customContext = Collections.unmodifiableMap(new HashMap<>(builder.customContext));
    }

    /**
     * Creates a new builder for constructing ScriptGenerationContext.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a context from a ForemanEntity.
     */
    public static ScriptGenerationContext fromEntity(ForemanEntity entity) {
        Builder builder = builder()
            .agentId(entity.getUUID().toString())
            .agentName(entity.getName().getString())
            .agentPosition(entity.blockPosition());

        // Note: Inventory access would need to be added to ForemanEntity
        // For now, inventory context can be added manually via builder

        // Add world context
        Level level = entity.level();
        if (level != null) {
            BlockPos pos = entity.blockPosition();
            builder.biome(level.getBiome(pos).toString());

            long dayTime = level.getDayTime() % 24000;
            if (dayTime < 12000) {
                builder.timeOfDay("day");
            } else if (dayTime < 14000) {
                builder.timeOfDay("sunset");
            } else if (dayTime < 22000) {
                builder.timeOfDay("night");
            } else {
                builder.timeOfDay("sunrise");
            }

            // Scan nearby blocks
            int scanRadius = 16;
            for (int x = -scanRadius; x <= scanRadius; x += 4) {
                for (int y = -scanRadius; y <= scanRadius; y += 4) {
                    for (int z = -scanRadius; z <= scanRadius; z += 4) {
                        BlockPos checkPos = pos.offset(x, y, z);
                        BlockState blockState = level.getBlockState(checkPos);
                        if (!blockState.isAir()) {
                            builder.addNearbyBlock(
                                blockState.getBlock().toString(),
                                checkPos,
                                pos.distManhattan(checkPos)
                            );
                        }
                    }
                }
            }
        }

        return builder.build();
    }

    /**
     * Checks if the context is empty.
     */
    public boolean isEmpty() {
        return agentState.isEmpty() &&
               inventory.isEmpty() &&
               nearbyBlocks.isEmpty() &&
               customContext.isEmpty();
    }

    /**
     * Converts the context to a prompt section for LLM.
     */
    public String toPromptSection() {
        StringBuilder sb = new StringBuilder(2048);

        // Agent information
        if (agentId != null || agentName != null) {
            sb.append("**Agent:**\n");
            if (agentName != null) {
                sb.append("- Name: ").append(agentName).append("\n");
            }
            if (agentPosition != null) {
                sb.append("- Position: ")
                  .append("X=").append(agentPosition.getX())
                  .append(", Y=").append(agentPosition.getY())
                  .append(", Z=").append(agentPosition.getZ())
                  .append("\n");
            }
            sb.append("\n");
        }

        // Agent state
        if (!agentState.isEmpty()) {
            sb.append("**Agent State:**\n");
            for (Map.Entry<String, Object> entry : agentState.entrySet()) {
                sb.append("- ").append(entry.getKey())
                  .append(": ").append(entry.getValue()).append("\n");
            }
            sb.append("\n");
        }

        // Inventory
        if (!inventory.isEmpty()) {
            sb.append("**Inventory:**\n");
            inventory.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .forEach(entry -> {
                    sb.append("- ").append(entry.getKey())
                      .append(": ").append(entry.getValue()).append("\n");
                });
            sb.append("\n");
        }

        // Nearby blocks
        if (!nearbyBlocks.isEmpty()) {
            sb.append("**Nearby Blocks:**\n");
            nearbyBlocks.stream()
                .limit(20)  // Limit to avoid overwhelming the prompt
                .forEach(block -> {
                    sb.append("- ").append(block.blockType())
                      .append(" at ").append(block.position())
                      .append(" (distance: ").append(block.distance()).append(")\n");
                });
            sb.append("\n");
        }

        // Environment
        if (biome != null || timeOfDay != null) {
            sb.append("**Environment:**\n");
            if (biome != null) {
                sb.append("- Biome: ").append(biome).append("\n");
            }
            if (timeOfDay != null) {
                sb.append("- Time: ").append(timeOfDay).append("\n");
            }
            sb.append("\n");
        }

        // Custom context
        if (!customContext.isEmpty()) {
            sb.append("**Additional Context:**\n");
            for (Map.Entry<String, Object> entry : customContext.entrySet()) {
                sb.append("- ").append(entry.getKey())
                  .append(": ").append(entry.getValue()).append("\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    // Getters
    public String getAgentId() { return agentId; }
    public String getAgentName() { return agentName; }
    public BlockPos getAgentPosition() { return agentPosition; }
    public Map<String, Object> getAgentState() { return agentState; }
    public Map<String, Integer> getInventory() { return inventory; }
    public List<NearbyBlock> getNearbyBlocks() { return nearbyBlocks; }
    public String getBiome() { return biome; }
    public String getTimeOfDay() { return timeOfDay; }
    public Map<String, Object> getCustomContext() { return customContext; }

    /**
     * Builder for constructing ScriptGenerationContext instances.
     */
    public static class Builder {
        private String agentId;
        private String agentName;
        private BlockPos agentPosition;
        private Map<String, Object> agentState = new HashMap<>();
        private Map<String, Integer> inventory = new HashMap<>();
        private List<NearbyBlock> nearbyBlocks = new ArrayList<>();
        private String biome;
        private String timeOfDay;
        private Map<String, Object> customContext = new HashMap<>();

        public Builder agentId(String agentId) {
            this.agentId = agentId;
            return this;
        }

        public Builder agentName(String agentName) {
            this.agentName = agentName;
            return this;
        }

        public Builder agentPosition(BlockPos position) {
            this.agentPosition = position;
            return this;
        }

        public Builder agentPosition(int x, int y, int z) {
            this.agentPosition = new BlockPos(x, y, z);
            return this;
        }

        public Builder addAgentState(String key, Object value) {
            this.agentState.put(key, value);
            return this;
        }

        public Builder agentState(Map<String, Object> state) {
            this.agentState = new HashMap<>(state);
            return this;
        }

        public Builder addInventoryItem(String item, int count) {
            this.inventory.put(item, count);
            return this;
        }

        public Builder inventory(Map<String, Integer> inventory) {
            this.inventory = new HashMap<>(inventory);
            return this;
        }

        public Builder addNearbyBlock(String blockType, BlockPos position, int distance) {
            this.nearbyBlocks.add(new NearbyBlock(blockType, position, distance));
            return this;
        }

        public Builder nearbyBlocks(List<NearbyBlock> blocks) {
            this.nearbyBlocks = new ArrayList<>(blocks);
            return this;
        }

        public Builder biome(String biome) {
            this.biome = biome;
            return this;
        }

        public Builder timeOfDay(String timeOfDay) {
            this.timeOfDay = timeOfDay;
            return this;
        }

        public Builder addCustomContext(String key, Object value) {
            this.customContext.put(key, value);
            return this;
        }

        public Builder customContext(Map<String, Object> context) {
            this.customContext = new HashMap<>(context);
            return this;
        }

        public ScriptGenerationContext build() {
            return new ScriptGenerationContext(this);
        }
    }

    /**
     * Information about a nearby block.
     */
    public record NearbyBlock(
        String blockType,
        BlockPos position,
        int distance
    ) {
        @Override
        public String toString() {
            return blockType + " at [" + position.getX() + ", " + position.getY() + ", " + position.getZ() + "]";
        }
    }
}
