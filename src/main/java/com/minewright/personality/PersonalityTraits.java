package com.minewright.personality;

import java.util.Objects;

/**
 * Represents personality traits using the OCEAN model (Big Five personality traits).
 *
 * <p>The OCEAN model is a widely accepted taxonomy for personality traits:</p>
 * <ul>
 *   <li><b>Openness</b> - Creativity, curiosity, preference for variety</li>
 *   <li><b>Conscientiousness</b> - Organization, dependability, discipline</li>
 *   <li><b>Extraversion</b> - Sociability, talkativeness, assertiveness</li>
 *   <li><b>Agreeableness</b> - Trust, altruism, cooperation</li>
 *   <li><b>Neuroticism</b> - Emotional stability, anxiety, moodiness</li>
 * </ul>
 *
 * <p>All values are represented as integers from 0-100, where:</p>
 * <ul>
 *   <li>0-20: Very low</li>
 *   <li>21-40: Low</li>
 *   <li>41-60: Average</li>
 *   <li>61-80: High</li>
 *   <li>81-100: Very high</li>
 * </ul>
 *
 * @since 1.3.0
 * @see ArtificerArchetype
 */
public class PersonalityTraits {

    private final int openness;
    private final int conscientiousness;
    private final int extraversion;
    private final int agreeableness;
    private final int neuroticism;

    /**
     * Creates a new PersonalityTraits instance with all OCEAN values.
     *
     * @param openness Creativity and curiosity (0-100)
     * @param conscientiousness Organization and discipline (0-100)
     * @param extraversion Sociability and assertiveness (0-100)
     * @param agreeableness Trust and cooperation (0-100)
     * @param neuroticism Emotional instability (0-100)
     * @throws IllegalArgumentException if any value is not in range 0-100
     */
    public PersonalityTraits(
            int openness,
            int conscientiousness,
            int extraversion,
            int agreeableness,
            int neuroticism) {
        validateTrait("openness", openness);
        validateTrait("conscientiousness", conscientiousness);
        validateTrait("extraversion", extraversion);
        validateTrait("agreeableness", agreeableness);
        validateTrait("neuroticism", neuroticism);

        this.openness = openness;
        this.conscientiousness = conscientiousness;
        this.extraversion = extraversion;
        this.agreeableness = agreeableness;
        this.neuroticism = neuroticism;
    }

    private void validateTrait(String name, int value) {
        if (value < 0 || value > 100) {
            throw new IllegalArgumentException(
                String.format("%s must be between 0 and 100, got: %d", name, value)
            );
        }
    }

    /**
     * Returns the openness trait value.
     * <p>High openness indicates creativity and preference for novelty.</p>
     *
     * @return Openness value (0-100)
     */
    public int getOpenness() {
        return openness;
    }

    /**
     * Returns the conscientiousness trait value.
     * <p>High conscientiousness indicates organization and reliability.</p>
     *
     * @return Conscientiousness value (0-100)
     */
    public int getConscientiousness() {
        return conscientiousness;
    }

    /**
     * Returns the extraversion trait value.
     * <p>High extraversion indicates sociability and enthusiasm.</p>
     *
     * @return Extraversion value (0-100)
     */
    public int getExtraversion() {
        return extraversion;
    }

    /**
     * Returns the agreeableness trait value.
     * <p>High agreeableness indicates cooperativeness and trust.</p>
     *
     * @return Agreeableness value (0-100)
     */
    public int getAgreeableness() {
        return agreeableness;
    }

    /**
     * Returns the neuroticism trait value.
     * <p>High neuroticism indicates emotional sensitivity and anxiety.</p>
     *
     * @return Neuroticism value (0-100)
     */
    public int getNeuroticism() {
        return neuroticism;
    }

    /**
     * Returns a human-readable description of the trait level.
     *
     * @param value The trait value to describe
     * @return Description string
     */
    public static String getTraitLevelDescription(int value) {
        if (value <= 20) return "Very Low";
        if (value <= 40) return "Low";
        if (value <= 60) return "Average";
        if (value <= 80) return "High";
        return "Very High";
    }

    /**
     * Creates a new PersonalityTraits instance by blending this with another.
     * <p>The blend is a weighted average where weight determines the influence
     * of the other traits (0.0 = no change, 1.0 = equal blend).</p>
     *
     * @param other The traits to blend with
     * @param weight The blend weight (0.0 to 1.0)
     * @return A new PersonalityTraits instance representing the blend
     * @throws IllegalArgumentException if weight is not in range 0-1
     */
    public PersonalityTraits blend(PersonalityTraits other, double weight) {
        if (weight < 0.0 || weight > 1.0) {
            throw new IllegalArgumentException("weight must be between 0.0 and 1.0, got: " + weight);
        }

        double otherWeight = weight;
        double selfWeight = 1.0 - weight;

        return new PersonalityTraits(
            (int) Math.round(this.openness * selfWeight + other.openness * otherWeight),
            (int) Math.round(this.conscientiousness * selfWeight + other.conscientiousness * otherWeight),
            (int) Math.round(this.extraversion * selfWeight + other.extraversion * otherWeight),
            (int) Math.round(this.agreeableness * selfWeight + other.agreeableness * otherWeight),
            (int) Math.round(this.neuroticism * selfWeight + other.neuroticism * otherWeight)
        );
    }

    /**
     * Returns a formatted string representation of all traits.
     *
     * @return Multi-line string with trait descriptions
     */
    public String toDetailedString() {
        return String.format(
            "PersonalityTraits:\n" +
            "  Openness: %d (%s)\n" +
            "  Conscientiousness: %d (%s)\n" +
            "  Extraversion: %d (%s)\n" +
            "  Agreeableness: %d (%s)\n" +
            "  Neuroticism: %d (%s)",
            openness, getTraitLevelDescription(openness),
            conscientiousness, getTraitLevelDescription(conscientiousness),
            extraversion, getTraitLevelDescription(extraversion),
            agreeableness, getTraitLevelDescription(agreeableness),
            neuroticism, getTraitLevelDescription(neuroticism)
        );
    }

    @Override
    public String toString() {
        return String.format("O:%d C:%d E:%d A:%d N:%d",
            openness, conscientiousness, extraversion, agreeableness, neuroticism);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PersonalityTraits that = (PersonalityTraits) o;
        return openness == that.openness &&
               conscientiousness == that.conscientiousness &&
               extraversion == that.extraversion &&
               agreeableness == that.agreeableness &&
               neuroticism == that.neuroticism;
    }

    @Override
    public int hashCode() {
        return Objects.hash(openness, conscientiousness, extraversion, agreeableness, neuroticism);
    }

    /**
     * Creates a builder for constructing PersonalityTraits instances.
     *
     * @return A new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder pattern for constructing PersonalityTraits.
     */
    public static class Builder {
        private Integer openness;
        private Integer conscientiousness;
        private Integer extraversion;
        private Integer agreeableness;
        private Integer neuroticism;

        public Builder openness(int openness) {
            this.openness = openness;
            return this;
        }

        public Builder conscientiousness(int conscientiousness) {
            this.conscientiousness = conscientiousness;
            return this;
        }

        public Builder extraversion(int extraversion) {
            this.extraversion = extraversion;
            return this;
        }

        public Builder agreeableness(int agreeableness) {
            this.agreeableness = agreeableness;
            return this;
        }

        public Builder neuroticism(int neuroticism) {
            this.neuroticism = neuroticism;
            return this;
        }

        /**
         * Builds the PersonalityTraits instance.
         * All values must be set (non-null) before calling this method.
         *
         * @return A new PersonalityTraits instance
         * @throws IllegalStateException if any value is null
         */
        public PersonalityTraits build() {
            if (openness == null || conscientiousness == null || extraversion == null ||
                agreeableness == null || neuroticism == null) {
                throw new IllegalStateException("All trait values must be set before building");
            }
            return new PersonalityTraits(openness, conscientiousness, extraversion,
                                       agreeableness, neuroticism);
        }
    }
}
