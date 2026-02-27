package com.minewright.personality;

import java.util.Objects;

/**
 * Represents the result of blending two {@link ArtificerArchetype} instances.
 *
 * <p>Blend operations combine personality traits, communication styles, and catchphrases
 * from two archetypes using a weighted average. This enables dynamic personality creation
 * and archetype customization.</p>
 *
 * <p>The result includes:</p>
 * <ul>
 *   <li>Blended personality traits (OCEAN model)</li>
 *   <li>Blended communication metrics (formality, humor, encouragement)</li>
 *   <li>Merged catchphrase lists from both archetypes</li>
 *   <li>Metadata about the blend operation</li>
 * </ul>
 *
 * @since 1.3.0
 * @see ArtificerArchetype#blend(ArtificerArchetype, double)
 */
public class BlendResult {

    private final ArtificerArchetype primary;
    private final ArtificerArchetype secondary;
    private final double blendWeight;
    private final PersonalityTraits blendedTraits;
    private final int blendedFormality;
    private final int blendedHumor;
    private final int blendedEncouragement;

    /**
     * Creates a new BlendResult.
     *
     * @param primary The primary archetype (base)
     * @param secondary The secondary archetype (to blend in)
     * @param blendWeight The weight of the secondary archetype (0.0 to 1.0)
     * @param blendedTraits The resulting blended personality traits
     * @param blendedFormality The resulting blended formality (0-100)
     * @param blendedHumor The resulting blended humor (0-100)
     * @param blendedEncouragement The resulting blended encouragement (0-100)
     */
    public BlendResult(
            ArtificerArchetype primary,
            ArtificerArchetype secondary,
            double blendWeight,
            PersonalityTraits blendedTraits,
            int blendedFormality,
            int blendedHumor,
            int blendedEncouragement) {
        this.primary = primary;
        this.secondary = secondary;
        this.blendWeight = blendWeight;
        this.blendedTraits = blendedTraits;
        this.blendedFormality = blendedFormality;
        this.blendedHumor = blendedHumor;
        this.blendedEncouragement = blendedEncouragement;
    }

    /**
     * Returns the primary archetype used in the blend.
     * <p>This archetype had the dominant influence (1.0 - weight).</p>
     *
     * @return The primary archetype
     */
    public ArtificerArchetype getPrimary() {
        return primary;
    }

    /**
     * Returns the secondary archetype used in the blend.
     * <p>This archetype contributed based on the blend weight.</p>
     *
     * @return The secondary archetype
     */
    public ArtificerArchetype getSecondary() {
        return secondary;
    }

    /**
     * Returns the blend weight applied to the secondary archetype.
     *
     * @return The weight (0.0 to 1.0)
     */
    public double getBlendWeight() {
        return blendWeight;
    }

    /**
     * Returns the blended personality traits.
     *
     * @return A new PersonalityTraits instance representing the blend
     */
    public PersonalityTraits getBlendedTraits() {
        return blendedTraits;
    }

    /**
     * Returns the blended formality value.
     *
     * @return Formality value (0-100)
     */
    public int getBlendedFormality() {
        return blendedFormality;
    }

    /**
     * Returns the blended humor value.
     *
     * @return Humor value (0-100)
     */
    public int getBlendedHumor() {
        return blendedHumor;
    }

    /**
     * Returns the blended encouragement value.
     *
     * @return Encouragement value (0-100)
     */
    public int getBlendedEncouragement() {
        return blendedEncouragement;
    }

    /**
     * Returns a description of the blend ratio.
     *
     * @return Human-readable blend description
     */
    public String getBlendDescription() {
        if (blendWeight <= 0.0) {
            return String.format("Pure %s", primary.getName());
        } else if (blendWeight >= 1.0) {
            return String.format("Pure %s", secondary.getName());
        } else if (blendWeight <= 0.33) {
            return String.format("%s with hints of %s", primary.getName(), secondary.getName());
        } else if (blendWeight <= 0.66) {
            return String.format("Balanced %s-%s blend", primary.getName(), secondary.getName());
        } else {
            return String.format("%s with %s foundation", secondary.getName(), primary.getName());
        }
    }

    /**
     * Returns a formatted string representation suitable for logging or debugging.
     *
     * @return Multi-line string describing the blend result
     */
    public String toDetailedString() {
        return String.format(
            "BlendResult:\n" +
            "  Primary: %s (%s)\n" +
            "  Secondary: %s (%s)\n" +
            "  Weight: %.2f\n" +
            "  Description: %s\n" +
            "  Traits: %s\n" +
            "  Communication: Formality=%d, Humor=%d, Encouragement=%d",
            primary.getName(), primary.getTitle(),
            secondary.getName(), secondary.getTitle(),
            blendWeight,
            getBlendDescription(),
            blendedTraits,
            blendedFormality, blendedHumor, blendedEncouragement
        );
    }

    @Override
    public String toString() {
        return String.format("BlendResult[%s + %s (%.0f%%)]",
            primary.getName(), secondary.getName(), blendWeight * 100);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlendResult that = (BlendResult) o;
        return Double.compare(that.blendWeight, blendWeight) == 0 &&
               blendedFormality == that.blendedFormality &&
               blendedHumor == that.blendedHumor &&
               blendedEncouragement == that.blendedEncouragement &&
               primary == that.primary &&
               secondary == that.secondary &&
               Objects.equals(blendedTraits, that.blendedTraits);
    }

    @Override
    public int hashCode() {
        return Objects.hash(primary, secondary, blendWeight, blendedTraits,
                          blendedFormality, blendedHumor, blendedEncouragement);
    }

    /**
     * Creates a builder for constructing BlendResult instances.
     *
     * @return A new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder pattern for constructing BlendResult instances.
     */
    public static class Builder {
        private ArtificerArchetype primary;
        private ArtificerArchetype secondary;
        private Double blendWeight;
        private PersonalityTraits blendedTraits;
        private Integer blendedFormality;
        private Integer blendedHumor;
        private Integer blendedEncouragement;

        public Builder primary(ArtificerArchetype primary) {
            this.primary = primary;
            return this;
        }

        public Builder secondary(ArtificerArchetype secondary) {
            this.secondary = secondary;
            return this;
        }

        public Builder blendWeight(double blendWeight) {
            this.blendWeight = blendWeight;
            return this;
        }

        public Builder blendedTraits(PersonalityTraits blendedTraits) {
            this.blendedTraits = blendedTraits;
            return this;
        }

        public Builder blendedFormality(int blendedFormality) {
            this.blendedFormality = blendedFormality;
            return this;
        }

        public Builder blendedHumor(int blendedHumor) {
            this.blendedHumor = blendedHumor;
            return this;
        }

        public Builder blendedEncouragement(int blendedEncouragement) {
            this.blendedEncouragement = blendedEncouragement;
            return this;
        }

        /**
         * Builds the BlendResult instance.
         * All fields must be set before calling this method.
         *
         * @return A new BlendResult instance
         * @throws IllegalStateException if any field is null
         */
        public BlendResult build() {
            if (primary == null || secondary == null || blendWeight == null ||
                blendedTraits == null || blendedFormality == null ||
                blendedHumor == null || blendedEncouragement == null) {
                throw new IllegalStateException("All fields must be set before building");
            }
            return new BlendResult(primary, secondary, blendWeight, blendedTraits,
                                 blendedFormality, blendedHumor, blendedEncouragement);
        }
    }
}
