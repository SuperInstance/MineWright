package com.minewright.animal;

import net.minecraft.world.entity.EntityType;

/**
 * Classification system for Minecraft animals.
 *
 * <p>Animals are categorized by their behavior and player interaction patterns.
 * This classification drives AI decision-making for taming, breeding, and management.</p>
 *
 * @see AnimalDetector
 * @see PetManager
 * @since 1.0.0
 */
public enum AnimalClassification {

    /**
     * Animals that can be tamed and become loyal to a player.
     * Once tamed, these animals follow commands and protect their owner.
     */
    TAMABLE_PET("tamable_pet",
        EntityType.WOLF,
        EntityType.CAT,
        EntityType.PARROT
    ),

    /**
     * Animals primarily kept for resources (food, wool, etc.).
     * These can be bred to produce offspring and farm products.
     */
    BREEDABLE_LIVESTOCK("breedable_livestock",
        EntityType.COW,
        EntityType.SHEEP,
        EntityType.PIG,
        EntityType.CHICKEN,
        EntityType.GOAT,
        EntityType.RABBIT
    ),

    /**
     * Rideable animals that can be mounted for transportation.
     * Some can also be equipped with chests for storage.
     */
    MOUNT("mount",
        EntityType.HORSE,
        EntityType.DONKEY,
        EntityType.MULE,
        EntityType.CAMEL,
        EntityType.LLAMA
    ),

    /**
     * Aggressive mobs that attack players on sight.
     * These should be avoided or engaged in combat.
     */
    HOSTILE("hostile",
        EntityType.ZOMBIE,
        EntityType.SKELETON,
        EntityType.SPIDER,
        EntityType.CREEPER,
        EntityType.ENDERMAN,
        EntityType.WITCH
    ),

    /**
     * Animals that are normally peaceful but attack when provoked.
     * Exercise caution when approaching these.
     */
    NEUTRAL("neutral",
        EntityType.WOLF,
        EntityType.POLAR_BEAR,
        EntityType.PANDA,
        EntityType.DOLPHIN,
        EntityType.IRON_GOLEM,
        EntityType.LLAMA
    ),

    /**
     * Peaceful animals that never attack under any circumstances.
     * Safe to approach and interact with.
     */
    PASSIVE("passive",
        EntityType.COW,
        EntityType.SHEEP,
        EntityType.PIG,
        EntityType.CHICKEN,
        EntityType.RABBIT,
        EntityType.BAT,
        EntityType.SQUID,
        EntityType.VILLAGER
    );

    private final String category;
    private final EntityType<?>[] entityTypes;

    AnimalClassification(String category, EntityType<?>... entityTypes) {
        this.category = category;
        this.entityTypes = entityTypes;
    }

    public String getCategory() {
        return category;
    }

    /**
     * Classify an entity into an animal category.
     *
     * @param entity The entity to classify
     * @return The animal classification, or PASSIVE if unknown
     */
    public static AnimalClassification classify(net.minecraft.world.entity.Entity entity) {
        for (AnimalClassification classification : values()) {
            for (EntityType<?> type : classification.entityTypes) {
                if (entity.getType() == type) {
                    return classification;
                }
            }
        }
        return PASSIVE;
    }

    /**
     * Check if this animal type can be tamed.
     *
     * @return true if the animal can be tamed
     */
    public boolean isTamable() {
        return this == TAMABLE_PET || this == MOUNT;
    }

    /**
     * Check if this animal type can be bred.
     *
     * @return true if the animal can produce offspring
     */
    public boolean isBreedable() {
        return this == BREEDABLE_LIVESTOCK || this == TAMABLE_PET || this == MOUNT;
    }

    /**
     * Check if this animal type is dangerous.
     *
     * @return true if the animal poses a threat
     */
    public boolean isDangerous() {
        return this == HOSTILE || this == NEUTRAL;
    }
}
