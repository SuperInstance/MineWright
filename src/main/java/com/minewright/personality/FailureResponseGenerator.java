package com.minewright.personality;

/**
 * Generates personality-appropriate dialogue responses for failures, mistakes, and setbacks.
 *
 * <p>This system combines:</p>
 * <ul>
 *   <li>Failure severity classification (0-100)</li>
 *   <li>OCEAN personality trait analysis</li>
 *   <li>Emotional state tracking</li>
 *   <li>Learning history integration</li>
 *   <li>Dignity preservation patterns</li>
 * </ul>
 *
 * <p>The goal is to transform failures into opportunities for character development,
 * making AI companions feel more human and emotionally intelligent.</p>
 *
 * <p>This class has been refactored to delegate specialized tasks to focused components:</p>
 * <ul>
 *   <li>{@link FailureAnalyzer} - Analyzes failure types and contexts</li>
 *   <li>{@link ResponseTemplateManager} - Manages personality-specific response templates</li>
 *   <li>{@link PersonalityResponseInjector} - Applies personality to specialized scenarios</li>
 *   <li>{@link LearningAndRecoveryGenerator} - Generates learning and recovery content</li>
 * </ul>
 *
 * @see PersonalityTraits
 * @since 1.3.0
 */
public class FailureResponseGenerator {

    /**
     * Represents the severity level of a failure.
     */
    public enum SeverityLevel {
        MINOR_HICCUP(0, 20, "Minor Hiccup"),
        MODERATE_SETBACK(21, 40, "Moderate Setback"),
        SIGNIFICANT_FAILURE(41, 60, "Significant Failure"),
        CRITICAL_DISASTER(61, 100, "Critical Disaster");

        private final int minSeverity;
        private final int maxSeverity;
        private final String displayName;

        SeverityLevel(int minSeverity, int maxSeverity, String displayName) {
            this.minSeverity = minSeverity;
            this.maxSeverity = maxSeverity;
            this.displayName = displayName;
        }

        public static SeverityLevel fromSeverity(int severity) {
            for (SeverityLevel level : values()) {
                if (severity >= level.minSeverity && severity <= level.maxSeverity) {
                    return level;
                }
            }
            return CRITICAL_DISASTER;
        }

        public int getMinSeverity() { return minSeverity; }
        public int getMaxSeverity() { return maxSeverity; }
        public String getDisplayName() { return displayName; }
    }

    /**
     * Represents the type of failure that occurred.
     */
    public enum FailureType {
        RESOURCE_WASTE("Wasted resources or materials"),
        TOOL_BREAKAGE("Broke a tool from improper use"),
        NAVIGATION_ERROR("Got lost or took wrong path"),
        STRUCTURAL_FAILURE("Build collapsed or failed"),
        ITEM_LOSS("Lost important items"),
        TASK_FAILURE("Failed to complete assigned task"),
        COMMUNICATION_ERROR("Misunderstood instructions"),
        SAFETY_VIOLATION("Endangered self or others"),
        REPETITIVE_MISTAKE("Repeated a previous failure"),
        EMBARRASSING_MOMENT("Embarrassing public failure");

        private final String description;

        FailureType(String description) {
            this.description = description;
        }

        public String getDescription() { return description; }
    }

    /**
     * Represents the current emotional state affecting responses.
     */
    public enum EmotionalState {
        CALM("Balanced, composed"),
        ANXIOUS("Worried, fearful"),
        FRUSTRATED("Irritated, discouraged"),
        DETERMINED("Focused, motivated"),
        ASHAMED("Embarrassed, guilty"),
        CONFIDENT("Self-assured, capable"),
        HOPEFUL("Optimistic, forward-looking");

        private final String description;

        EmotionalState(String description) {
            this.description = description;
        }

        public String getDescription() { return description; }
    }

    /**
     * Context for generating a failure response.
     */
    public static class FailureContext {
        private final FailureType failureType;
        private final int severity;
        private final PersonalityTraits personality;
        private final EmotionalState emotionalState;
        private final int previousFailureCount;
        private final String specificContext;
        private final boolean playerIntervention;

        private FailureContext(Builder builder) {
            this.failureType = builder.failureType;
            this.severity = builder.severity;
            this.personality = builder.personality;
            this.emotionalState = builder.emotionalState;
            this.previousFailureCount = builder.previousFailureCount;
            this.specificContext = builder.specificContext;
            this.playerIntervention = builder.playerIntervention;
        }

        public FailureType getFailureType() { return failureType; }
        public int getSeverity() { return severity; }
        public PersonalityTraits getPersonality() { return personality; }
        public EmotionalState getEmotionalState() { return emotionalState; }
        public int getPreviousFailureCount() { return previousFailureCount; }
        public String getSpecificContext() { return specificContext; }
        public boolean hasPlayerIntervention() { return playerIntervention; }
        public SeverityLevel getSeverityLevel() { return SeverityLevel.fromSeverity(severity); }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private FailureType failureType;
            private int severity;
            private PersonalityTraits personality;
            private EmotionalState emotionalState = EmotionalState.CALM;
            private int previousFailureCount = 0;
            private String specificContext = "";
            private boolean playerIntervention = false;

            public Builder failureType(FailureType failureType) {
                this.failureType = failureType;
                return this;
            }

            public Builder severity(int severity) {
                this.severity = Math.max(0, Math.min(100, severity));
                return this;
            }

            public Builder personality(PersonalityTraits personality) {
                this.personality = personality;
                return this;
            }

            public Builder emotionalState(EmotionalState emotionalState) {
                this.emotionalState = emotionalState;
                return this;
            }

            public Builder previousFailureCount(int count) {
                this.previousFailureCount = Math.max(0, count);
                return this;
            }

            public Builder specificContext(String context) {
                this.specificContext = context != null ? context : "";
                return this;
            }

            public Builder playerIntervention(boolean playerIntervention) {
                this.playerIntervention = playerIntervention;
                return this;
            }

            public FailureContext build() {
                if (failureType == null || personality == null) {
                    throw new IllegalStateException("failureType and personality are required");
                }
                return new FailureContext(this);
            }
        }
    }

    /**
     * The generated response with metadata.
     */
    public static class FailureResponse {
        private final String dialogue;
        private final SeverityLevel severityLevel;
        private final String learningStatement;
        private final String recoveryPlan;
        private final boolean needsPlayerReassurance;
        private final long timestamp;

        private FailureResponse(Builder builder) {
            this.dialogue = builder.dialogue;
            this.severityLevel = builder.severityLevel;
            this.learningStatement = builder.learningStatement;
            this.recoveryPlan = builder.recoveryPlan;
            this.needsPlayerReassurance = builder.needsPlayerReassurance;
            this.timestamp = System.currentTimeMillis();
        }

        public String getDialogue() { return dialogue; }
        public SeverityLevel getSeverityLevel() { return severityLevel; }
        public String getLearningStatement() { return learningStatement; }
        public String getRecoveryPlan() { return recoveryPlan; }
        public boolean needsPlayerReassurance() { return needsPlayerReassurance; }
        public long getTimestamp() { return timestamp; }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String dialogue;
            private SeverityLevel severityLevel;
            private String learningStatement = "";
            private String recoveryPlan = "";
            private boolean needsPlayerReassurance = false;

            public Builder dialogue(String dialogue) {
                this.dialogue = dialogue;
                return this;
            }

            public Builder severityLevel(SeverityLevel severityLevel) {
                this.severityLevel = severityLevel;
                return this;
            }

            public Builder learningStatement(String learningStatement) {
                this.learningStatement = learningStatement;
                return this;
            }

            public Builder recoveryPlan(String recoveryPlan) {
                this.recoveryPlan = recoveryPlan;
                return this;
            }

            public Builder needsPlayerReassurance(boolean needs) {
                this.needsPlayerReassurance = needs;
                return this;
            }

            public FailureResponse build() {
                if (dialogue == null || severityLevel == null) {
                    throw new IllegalStateException("dialogue and severityLevel are required");
                }
                return new FailureResponse(this);
            }
        }
    }

    // Main generation method
    /**
     * Generates a personality-appropriate failure response.
     *
     * <p>This method coordinates the response generation process by delegating
     * to specialized components:</p>
     * <ul>
     *   <li>Uses {@link FailureAnalyzer} to determine archetypes and analyze context</li>
     *   <li>Uses {@link ResponseTemplateManager} to generate personality-specific dialogue</li>
     *   <li>Uses {@link LearningAndRecoveryGenerator} for learning statements and recovery plans</li>
     *   <li>Uses {@link FailureAnalyzer#needsPlayerReassurance(FailureContext)} for reassurance logic</li>
     * </ul>
     *
     * @param context The failure context including type, severity, and personality
     * @return A FailureResponse containing dialogue, learning statements, and recovery plans
     */
    public static FailureResponse generateResponse(FailureContext context) {
        SeverityLevel severityLevel = context.getSeverityLevel();

        // Generate the main dialogue based on personality and severity
        String dialogue = generateDialogue(context);

        // Generate learning statement if this is a repeated failure
        String learningStatement = "";
        if (context.getPreviousFailureCount() > 0 || context.getSeverity() >= 40) {
            learningStatement = LearningAndRecoveryGenerator.generateLearningStatement(context);
        }

        // Generate recovery plan for moderate+ failures
        String recoveryPlan = "";
        if (context.getSeverity() >= 21) {
            recoveryPlan = LearningAndRecoveryGenerator.generateRecoveryPlan(context);
        }

        // Determine if player reassurance is needed
        boolean needsReassurance = FailureAnalyzer.needsPlayerReassurance(context);

        return FailureResponse.builder()
                .dialogue(dialogue)
                .severityLevel(severityLevel)
                .learningStatement(learningStatement)
                .recoveryPlan(recoveryPlan)
                .needsPlayerReassurance(needsReassurance)
                .build();
    }

    /**
     * Generates the main dialogue response by delegating to template managers.
     */
    private static String generateDialogue(FailureContext context) {
        FailureAnalyzer.ResponseArchetype archetype =
            FailureAnalyzer.determineArchetype(context.getPersonality());

        switch (archetype) {
            case PERFECTIONIST:
                return ResponseTemplateManager.generatePerfectionistResponse(context);
            case WORRIER:
                return ResponseTemplateManager.generateWorrierResponse(context);
            case STOIC:
                return ResponseTemplateManager.generateStoicResponse(context);
            case ENTHUSIASTIC:
                return ResponseTemplateManager.generateEnthusiasticResponse(context);
            case INTROVERTED:
                return ResponseTemplateManager.generateIntrovertedResponse(context);
            case ACCOMMODATING:
                return ResponseTemplateManager.generateAccommodatingResponse(context);
            case DIRECT:
                return ResponseTemplateManager.generateDirectResponse(context);
            case INNOVATOR:
                return ResponseTemplateManager.generateInnovatorResponse(context);
            case BALANCED:
            default:
                return ResponseTemplateManager.generateBalancedResponse(context);
        }
    }

    /**
     * Generates a help request when the character needs assistance.
     * Delegates to {@link PersonalityResponseInjector}.
     */
    public static String generateHelpRequest(FailureContext context) {
        return PersonalityResponseInjector.generateHelpRequest(context);
    }

    /**
     * Generates a dignified response to an embarrassing failure.
     * Delegates to {@link PersonalityResponseInjector}.
     */
    public static String generateEmbarrassmentResponse(FailureContext context) {
        return PersonalityResponseInjector.generateEmbarrassmentResponse(context);
    }

    /**
     * Generates player reassurance dialogue.
     * Delegates to {@link PersonalityResponseInjector}.
     */
    public static String generatePlayerReassurance(FailureContext context) {
        return PersonalityResponseInjector.generatePlayerReassurance(context);
    }

    /**
     * Creates a failure context with sensible defaults for quick testing.
     */
    public static FailureContext createQuickContext(
            FailureType type,
            int severity,
            PersonalityTraits personality) {
        return FailureContext.builder()
                .failureType(type)
                .severity(severity)
                .personality(personality)
                .emotionalState(EmotionalState.CALM)
                .previousFailureCount(0)
                .playerIntervention(false)
                .build();
    }
}
