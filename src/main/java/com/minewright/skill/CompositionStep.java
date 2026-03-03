package com.minewright.skill;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a single step in a skill composition.
 *
 * <p><b>Purpose:</b></p>
 * <p>A CompositionStep wraps a skill with additional context variables
 * that are specific to that step's execution within a composition.</p>
 *
 * <p><b>Usage:</b></p>
 * <pre>
 * CompositionStep step = new CompositionStep(
 *     "stripMine",                    // Skill name
 *     SkillLibrary.getSkill("stripMine"), // Skill instance
 *     Map.of("depth", 50, "direction", "north")  // Step-specific context
 * );
 * </pre>
 *
 * @see ComposedSkill
 * @see SkillComposer
 * @since 1.1.0
 */
public class CompositionStep {
    private final String skillName;
    private final Skill skill;
    private final Map<String, Object> context;

    /**
     * Creates a new composition step.
     *
     * @param skillName Name of the skill (for reference and logging)
     * @param skill The skill instance (may be null if not yet resolved)
     * @param context Step-specific context variables
     */
    public CompositionStep(String skillName, Skill skill, Map<String, Object> context) {
        this.skillName = Objects.requireNonNull(skillName, "Skill name cannot be null");
        this.skill = skill; // Can be null, resolved later
        this.context = context != null ? new HashMap<>(context) : new HashMap<>();
    }

    /**
     * Gets the skill name.
     *
     * @return Skill name
     */
    public String getSkillName() {
        return skillName;
    }

    /**
     * Gets the skill instance.
     *
     * @return Skill instance, or null if not resolved
     */
    public Skill getSkill() {
        return skill;
    }

    /**
     * Gets the step-specific context variables.
     *
     * @return Unmodifiable map of context variables
     */
    public Map<String, Object> getContext() {
        return Collections.unmodifiableMap(context);
    }

    /**
     * Checks if this step has a valid skill.
     *
     * @return true if skill is not null
     */
    public boolean hasSkill() {
        return skill != null;
    }

    /**
     * Creates a new step with the skill resolved from the library.
     *
     * @return New step with resolved skill, or this step if already resolved
     */
    public CompositionStep resolve() {
        if (skill != null) {
            return this;
        }
        Skill resolved = SkillLibrary.getInstance().getSkill(skillName);
        return new CompositionStep(skillName, resolved, context);
    }

    /**
     * Creates a builder for constructing composition steps.
     *
     * @param skillName Name of the skill
     * @return Builder instance
     */
    public static Builder builder(String skillName) {
        return new Builder(skillName);
    }

    /**
     * Builder for constructing composition steps.
     */
    public static class Builder {
        private final String skillName;
        private Skill skill;
        private final Map<String, Object> context = new HashMap<>();

        private Builder(String skillName) {
            this.skillName = skillName;
        }

        /**
         * Sets the skill instance directly.
         *
         * @param skill Skill instance
         * @return This builder
         */
        public Builder skill(Skill skill) {
            this.skill = skill;
            return this;
        }

        /**
         * Adds a context variable.
         *
         * @param key Context key
         * @param value Context value
         * @return This builder
         */
        public Builder context(String key, Object value) {
            this.context.put(key, value);
            return this;
        }

        /**
         * Adds multiple context variables.
         *
         * @param context Map of context variables
         * @return This builder
         */
        public Builder context(Map<String, Object> context) {
            this.context.putAll(context);
            return this;
        }

        /**
         * Builds the composition step.
         *
         * @return New CompositionStep instance
         */
        public CompositionStep build() {
            // Try to resolve skill from library if not set
            if (skill == null) {
                skill = SkillLibrary.getInstance().getSkill(skillName);
            }
            return new CompositionStep(skillName, skill, context);
        }
    }

    @Override
    public String toString() {
        return String.format("CompositionStep[skill=%s, context=%d vars]",
            skillName, context.size());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CompositionStep that = (CompositionStep) o;
        return skillName.equals(that.skillName) && context.equals(that.context);
    }

    @Override
    public int hashCode() {
        return Objects.hash(skillName, context);
    }
}
