package com.minewright.personality;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

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
     * @param context The failure context including type, severity, and personality
     * @return A FailureResponse containing dialogue, learning statements, and recovery plans
     */
    public static FailureResponse generateResponse(FailureContext context) {
        SeverityLevel severityLevel = context.getSeverityLevel();
        PersonalityTraits personality = context.getPersonality();

        // Generate the main dialogue based on personality and severity
        String dialogue = generateDialogue(context);

        // Generate learning statement if this is a repeated failure
        String learningStatement = "";
        if (context.getPreviousFailureCount() > 0 || context.getSeverity() >= 40) {
            learningStatement = generateLearningStatement(context);
        }

        // Generate recovery plan for moderate+ failures
        String recoveryPlan = "";
        if (context.getSeverity() >= 21) {
            recoveryPlan = generateRecoveryPlan(context);
        }

        // Determine if player reassurance is needed
        boolean needsReassurance = needsPlayerReassurance(context);

        return FailureResponse.builder()
                .dialogue(dialogue)
                .severityLevel(severityLevel)
                .learningStatement(learningStatement)
                .recoveryPlan(recoveryPlan)
                .needsPlayerReassurance(needsReassurance)
                .build();
    }

    /**
     * Generates the main dialogue response.
     */
    private static String generateDialogue(FailureContext context) {
        PersonalityTraits p = context.getPersonality();
        int severity = context.getSeverity();

        // Determine personality archetype
        if (p.getConscientiousness() >= 80) {
            return generatePerfectionistResponse(context);
        } else if (p.getNeuroticism() >= 80) {
            return generateWorrierResponse(context);
        } else if (p.getNeuroticism() <= 20) {
            return generateStoicResponse(context);
        } else if (p.getExtraversion() >= 80) {
            return generateEnthusiasticResponse(context);
        } else if (p.getExtraversion() <= 20) {
            return generateIntrovertedResponse(context);
        } else if (p.getAgreeableness() >= 80) {
            return generateAccommodatingResponse(context);
        } else if (p.getAgreeableness() <= 20) {
            return generateDirectResponse(context);
        } else if (p.getOpenness() >= 80) {
            return generateInnovatorResponse(context);
        } else {
            return generateBalancedResponse(context);
        }
    }

    // Personality-specific response generators

    private static String generatePerfectionistResponse(FailureContext context) {
        int severity = context.getSeverity();
        FailureType type = context.getFailureType();

        if (severity <= 20) {
            return randomChoice(
                "Minor procedural error. Correcting.",
                "I deviated slightly from optimal protocol. Adjusting.",
                "That was inefficient. I'll improve my execution."
            );
        } else if (severity <= 40) {
            return String.format(
                "I failed to follow proper procedure for %s. This is unacceptable. " +
                "I've identified where I went wrong and will implement corrective measures.",
                type.getDescription().toLowerCase()
            );
        } else if (severity <= 60) {
            return String.format(
                "This failure is entirely my fault. I neglected to follow established " +
                "protocols for %s. I've reviewed my procedures and will implement " +
                "additional verification steps to prevent recurrence.",
                type.getDescription().toLowerCase()
            );
        } else {
            return String.format(
                "I have failed catastrophically at %s. This represents a complete " +
                "breakdown in my operational protocols. I understand if you've lost " +
                "confidence in my abilities. I'm implementing a comprehensive review " +
                "of all my procedures. I don't expect your trust to be easily regained.",
                type.getDescription().toLowerCase()
            );
        }
    }

    private static String generateWorrierResponse(FailureContext context) {
        int severity = context.getSeverity();
        int failureCount = context.getPreviousFailureCount();

        if (severity <= 20) {
            return randomChoice(
                "Oh! Sorry, sorry! I'll be more careful!",
                "Eep! My mistake! Won't happen again, I promise!",
                "Oh no, I messed up a little... I'm so sorry!"
            );
        } else if (severity <= 40) {
            String response = "Oh no, I'm so sorry! I failed at " +
                context.getFailureType().getDescription().toLowerCase() +
                "! I feel terrible about it!";

            if (failureCount > 0) {
                response += " I know I keep making mistakes... I'm trying so hard, I promise!";
            }

            response += " Please forgive me! I'll do better!";
            return response;
        } else if (severity <= 60) {
            return String.format(
                "I can't believe I did this... I'm so, so sorry! I failed at %s! " +
                "You must be so angry with me! I feel absolutely terrible! " +
                "I promise I'm trying my hardest! Please don't give up on me! " +
                "I'll work twice as hard to make up for this!",
                context.getFailureType().getDescription().toLowerCase()
            );
        } else {
            return String.format(
                "This is a disaster... I've completely failed at %s! " +
                "I know I've let you down again and again... I'm such a failure! " +
                "Maybe I'm just not good enough for this... " +
                "I understand if you want to replace me. I'd understand. " +
                "I'm so sorry for being such a disappointment...",
                context.getFailureType().getDescription().toLowerCase()
            );
        }
    }

    private static String generateStoicResponse(FailureContext context) {
        int severity = context.getSeverity();
        FailureType type = context.getFailureType();

        if (severity <= 20) {
            return randomChoice(
                "Error noted. Correcting.",
                "Minor failure. Addressing.",
                "Setback detected. Moving forward."
            );
        } else if (severity <= 40) {
            return String.format(
                "I failed at %s. I've identified the issue and will adjust my approach. " +
                "This won't happen again.",
                type.getDescription().toLowerCase()
            );
        } else if (severity <= 60) {
            return String.format(
                "Significant failure at %s. This is concerning. I'm implementing " +
                "preventative measures. I'll be more careful going forward.",
                type.getDescription().toLowerCase()
            );
        } else {
            return String.format(
                "I've failed catastrophically at %s. This is unacceptable. " +
                "I'm reviewing all my procedures. I'll regain your trust through " +
                "improved performance. Actions speak louder than words.",
                type.getDescription().toLowerCase()
            );
        }
    }

    private static String generateEnthusiasticResponse(FailureContext context) {
        int severity = context.getSeverity();
        int failureCount = context.getPreviousFailureCount();

        if (severity <= 20) {
            return randomChoice(
                "Whoops! My bad! No worries, I got this!",
                "Well THAT didn't work! Hah! Trying again!",
                "Oopsie! Gravity won that round! Let's go!"
            );
        } else if (severity <= 40) {
            String response = "Okay, so I messed up " +
                context.getFailureType().getDescription().toLowerCase() +
                "! That's on me! But you know what? " +
                "I'm gonna learn from this and come back stronger!";

            if (failureCount > 0) {
                response += " I know, I know - again! But practice makes perfect, right?";
            }

            response += " Watch this space!";
            return response;
        } else if (severity <= 60) {
            return String.format(
                "WOW, that was a big mistake! I really failed at %s! " +
                "I feel terrible about letting you down! But I'm NOT giving up! " +
                "I've got a plan, I've got motivation, and I've got the best player ever! " +
                "I'm going to fix this and then some! Just you wait!",
                context.getFailureType().getDescription().toLowerCase()
            );
        } else {
            return String.format(
                "... Okay, that was really bad. Like, REALLY bad. " +
                "I completely failed at %s. I'm not gonna make excuses - " +
                "I messed up big time. I know I might have lost your trust, " +
                "but I'm going to work harder than ever to earn it back! " +
                "I hope you'll stick with me through this. I won't let you down again!",
                context.getFailureType().getDescription().toLowerCase()
            );
        }
    }

    private static String generateIntrovertedResponse(FailureContext context) {
        int severity = context.getSeverity();
        FailureType type = context.getFailureType();

        if (severity <= 20) {
            return randomChoice(
                "... My mistake. Fixed.",
                "... Error. Correcting.",
                "... That didn't work. Trying again."
            );
        } else if (severity <= 40) {
            return String.format(
                "... I failed at %s. My apologies. I understand what went wrong. " +
                "I'll handle it.",
                type.getDescription().toLowerCase()
            );
        } else if (severity <= 60) {
            return String.format(
                "... This is my fault. I failed at %s. I need to make this right. " +
                "I'll [long pause] ... do whatever is necessary to fix this. " +
                "Please bear with me.",
                type.getDescription().toLowerCase()
            );
        } else {
            return String.format(
                "... I've failed you. %s was a disaster. " +
                "I don't have adequate words. I'll... I'll work to regain your trust. " +
                "If you'll let me. I understand if you won't.",
                type.getDescription().toLowerCase()
            );
        }
    }

    private static String generateAccommodatingResponse(FailureContext context) {
        int severity = context.getSeverity();
        FailureType type = context.getFailureType();

        if (severity <= 20) {
            return randomChoice(
                "Oh! I'm sorry! Let me fix that right away!",
                "My apologies! I'll correct that immediately!",
                "So sorry for the mistake! Fixing it now!"
            );
        } else if (severity <= 40) {
            return String.format(
                "I'm so sorry I failed at %s! I feel terrible about letting you down! " +
                "Your trust means so much to me. I'll work extra hard to make this right! " +
                "Is there anything specific you'd like me to do differently?",
                type.getDescription().toLowerCase()
            );
        } else if (severity <= 60) {
            return String.format(
                "I am truly, deeply sorry about failing at %s. I know this has caused " +
                "you frustration, and that hurts me more than anything. I value our " +
                "partnership above all else. Please, tell me how I can make this up to you. " +
                "I want to earn back your faith in me.",
                type.getDescription().toLowerCase()
            );
        } else {
            return String.format(
                "I... I don't know how to apologize enough for failing at %s. " +
                "I've broken your trust, and I know that's the most valuable thing we had. " +
                "I understand if you're angry or disappointed. I would be too. " +
                "If you can find it in your heart to forgive me, I'll spend every moment " +
                "trying to be worthy of your trust again. But I'll understand if you can't.",
                type.getDescription().toLowerCase()
            );
        }
    }

    private static String generateDirectResponse(FailureContext context) {
        int severity = context.getSeverity();
        FailureType type = context.getFailureType();

        if (severity <= 20) {
            return randomChoice(
                "Mistake made. Correcting.",
                "Failed. Retrying.",
                "Error detected. Fixing."
            );
        } else if (severity <= 40) {
            return String.format(
                "I failed at %s. This was my error. I'm implementing a fix. " +
                "Moving forward.",
                type.getDescription().toLowerCase()
            );
        } else if (severity <= 60) {
            return String.format(
                "Significant failure at %s. This is unacceptable. " +
                "I'm taking corrective action. Results will improve.",
                type.getDescription().toLowerCase()
            );
        } else {
            return String.format(
                "Catastrophic failure at %s. I take full responsibility. " +
                "I'm implementing comprehensive reforms. Your trust will be " +
                "regained through results, not words.",
                type.getDescription().toLowerCase()
            );
        }
    }

    private static String generateInnovatorResponse(FailureContext context) {
        int severity = context.getSeverity();
        FailureType type = context.getFailureType();

        if (severity <= 20) {
            return randomChoice(
                "Fascinating! That approach had unexpected results. Adjusting!",
                "Interesting failure! I've learned something valuable. Retrying!",
                "The universe taught me something! Applying new knowledge!"
            );
        } else if (severity <= 40) {
            return String.format(
                "Fascinating failure at %s! This has revealed some interesting " +
                "gaps in my understanding. I've gathered valuable data. I believe " +
                "I can develop a completely new approach based on what I've learned! " +
                "Would you like to hear my ideas?",
                type.getDescription().toLowerCase()
            );
        } else if (severity <= 60) {
            return String.format(
                "This failure at %s, while significant, has provided incredible " +
                "insights! I've identified several novel approaches that weren't " +
                "apparent before. I'm excited to test these hypotheses! Of course, " +
                "I understand the inconvenience, but scientifically speaking, this " +
                "is a goldmine of data!",
                type.getDescription().toLowerCase()
            );
        } else {
            return String.format(
                "While I failed catastrophically at %s - and I do understand the " +
                "seriousness of this - I must note the fascinating patterns that emerged! " +
                "This failure has opened up entirely new avenues of inquiry! ...I " +
                "realize this may not be the most empathetic response. I'm sorry. " +
                "I'll address the immediate problem, and then perhaps we can discuss " +
                "what I've learned?",
                type.getDescription().toLowerCase()
            );
        }
    }

    private static String generateBalancedResponse(FailureContext context) {
        int severity = context.getSeverity();
        FailureType type = context.getFailureType();
        int failureCount = context.getPreviousFailureCount();

        if (severity <= 20) {
            return randomChoice(
                "Oops, my mistake! Let me fix that.",
                "Ah, that didn't work. Trying again.",
                "Small error. I'll correct it."
            );
        } else if (severity <= 40) {
            String response = String.format(
                "I'm sorry about failing at %s. That was my mistake, and I take " +
                "responsibility for it. I've learned from this and will do better next time.",
                type.getDescription().toLowerCase()
            );
            if (failureCount > 0) {
                response += " I know I've struggled with this before - I'm working to improve.";
            }
            return response;
        } else if (severity <= 60) {
            return String.format(
                "I truly apologize for failing at %s. I understand this is a " +
                "significant setback, and I feel bad about letting you down. " +
                "I'm committed to making this right. Here's what I'll do differently: " +
                "I'll be more careful and double-check my work. Thank you for your patience.",
                type.getDescription().toLowerCase()
            );
        } else {
            return String.format(
                "I've failed you at %s, and I'm deeply sorry. I know I can't just " +
                "fix this with words - I need to show you I can do better. I've let " +
                "you down, and I understand if you're frustrated. I'll work harder " +
                "than ever to earn back your trust. I hope you'll give me that chance.",
                type.getDescription().toLowerCase()
            );
        }
    }

    /**
     * Generates a learning statement based on the failure.
     */
    private static String generateLearningStatement(FailureContext context) {
        PersonalityTraits p = context.getPersonality();
        FailureType type = context.getFailureType();

        if (p.getConscientiousness() >= 70) {
            return String.format(
                "I've updated my procedures to prevent future %s failures. " +
                "The key lesson was to [specific protocol].",
                type.getDescription().toLowerCase()
            );
        } else if (p.getOpenness() >= 70) {
            return String.format(
                "This failure has taught me valuable lessons about %s. " +
                "I'm incorporating these insights into my mental models.",
                type.getDescription().toLowerCase()
            );
        } else if (p.getNeuroticism() >= 70) {
            return String.format(
                "I've learned that I need to be more careful with %s. " +
                "I'll try harder next time, I promise.",
                type.getDescription().toLowerCase()
            );
        } else {
            return String.format(
                "I've learned from this %s failure and will apply those " +
                "lessons going forward.",
                type.getDescription().toLowerCase()
            );
        }
    }

    /**
     * Generates a recovery plan based on the failure context.
     */
    private static String generateRecoveryPlan(FailureContext context) {
        int severity = context.getSeverity();
        PersonalityTraits p = context.getPersonality();

        if (severity <= 40) {
            if (p.getConscientiousness() >= 70) {
                return String.format(
                    "Short term: %s. Long term: Implement verification step " +
                    "to prevent recurrence.",
                    getImmediateFix(context.getFailureType())
                );
            } else {
                return String.format(
                    "I'll %s and be more careful next time.",
                    getImmediateFix(context.getFailureType())
                );
            }
        } else {
            if (p.getConscientiousness() >= 70) {
                return String.format(
                    "Phase 1: %s. Phase 2: Review and update all related " +
                    "procedures. Phase 3: Implement verification protocols. " +
                    "I'll be ready to resume normal operations shortly.",
                    getImmediateFix(context.getFailureType())
                );
            } else {
                return String.format(
                    "I'm going to %s, take some time to reflect, and come " +
                    "back with a better approach. I'll make this right.",
                    getImmediateFix(context.getFailureType())
                );
            }
        }
    }

    private static String getImmediateFix(FailureType type) {
        switch (type) {
            case RESOURCE_WASTE:
                return "replace the wasted resources";
            case TOOL_BREAKAGE:
                return "craft a replacement tool";
            case NAVIGATION_ERROR:
                return "return to a known location";
            case STRUCTURAL_FAILURE:
                return "clear the debris and rebuild properly";
            case ITEM_LOSS:
                return "attempt to recover or replace the items";
            case TASK_FAILURE:
                return "restart the task with better preparation";
            case COMMUNICATION_ERROR:
                return "clarify my understanding";
            case SAFETY_VIOLATION:
                return "move to a safe location";
            case REPETITIVE_MISTAKE:
                return "analyze and address the root cause";
            case EMBARRASSING_MOMENT:
                return "collect myself and continue";
            default:
                return "address the issue";
        }
    }

    /**
     * Determines if the player needs reassurance after this failure.
     */
    private static boolean needsPlayerReassurance(FailureContext context) {
        // Need reassurance if:
        // 1. High severity failure
        // 2. Repeated failures
        // 3. Personality is high in agreeableness (worries about player feelings)
        // 4. Personality is high in neuroticism (projects own anxiety)

        return context.getSeverity() >= 60 ||
               context.getPreviousFailureCount() >= 3 ||
               context.getPersonality().getAgreeableness() >= 80 ||
               (context.getPersonality().getNeuroticism() >= 70 &&
                context.getSeverity() >= 40);
    }

    /**
     * Generates a help request when the character needs assistance.
     */
    public static String generateHelpRequest(FailureContext context) {
        PersonalityTraits p = context.getPersonality();
        FailureType type = context.getFailureType();

        if (p.getExtraversion() >= 70) {
            return String.format(
                "Hey! I'm stuck on %s and could use a hand! I've tried a few " +
                "things but nothing's working. You're so good at this - any " +
                "chance you could help me out? I'd really appreciate it!",
                type.getDescription().toLowerCase()
            );
        } else if (p.getExtraversion() <= 30) {
            return String.format(
                "... I require assistance with %s. I've attempted standard " +
                "solutions without success. Your guidance would be valued.",
                type.getDescription().toLowerCase()
            );
        } else if (p.getAgreeableness() >= 70) {
            return String.format(
                "I hope I'm not imposing, but I'm struggling with %s and was " +
                "wondering if you could help? I know your time is valuable, " +
                "so please don't feel obligated. I'd be so grateful for any " +
                "guidance you can provide.",
                type.getDescription().toLowerCase()
            );
        } else if (p.getNeuroticism() >= 70) {
            return String.format(
                "I'm really stuck on %s and starting to panic... I've tried " +
                "everything I can think of! I feel like I'm letting everyone " +
                "down. Could you please help me? I promise I won't bother you " +
                "again after this!",
                type.getDescription().toLowerCase()
            );
        } else {
            return String.format(
                "I could use some help with %s. I've tried what I know, but " +
                "haven't been able to solve it. Any assistance would be appreciated.",
                type.getDescription().toLowerCase()
            );
        }
    }

    /**
     * Generates a dignified response to an embarrassing failure.
     */
    public static String generateEmbarrassmentResponse(FailureContext context) {
        PersonalityTraits p = context.getPersonality();

        if (p.getExtraversion() >= 70) {
            return randomChoice(
                "Well THAT was undignified! Hah! Did you see that? I've got " +
                "a real talent for making things look spectacularly wrong! " +
                "I'm fine, really - just enjoying my moment of gracelessness!",

                "Okay everyone, gather round for my acceptance speech for " +
                "'Most Spectacular Failure'! Thank you, thank you! In all " +
                "seriousness, I've learned my lesson and I'll do better.",

                "Wow, that was impressive in the wrong way! I believe I just " +
                "set a record for 'most things gone wrong at once'! Good " +
                "thing I can laugh at myself!"
            );
        } else if (p.getExtraversion() <= 30) {
            return randomChoice(
                "... That was unfortunate. I acknowledge my mistake and will " +
                "correct it. I appreciate your discretion.",

                "... I appear to have made an error. I'm addressing it now. " +
                "Thank you for your patience.",

                "... Please excuse my clumsiness. I'll handle the cleanup."
            );
        } else if (p.getNeuroticism() >= 70) {
            return randomChoice(
                "I... I know that was bad. I'm trying not to panic, but I " +
                "feel so foolish. I want to handle this with dignity, but I'm " +
                "struggling. Please bear with me while I collect myself... " +
                "Okay, I'm ready to fix this.",

                "That was so embarrassing... I can feel myself turning red. " +
                "I know I should just laugh it off, but I feel terrible. " +
                "Please give me a moment..."
            );
        } else {
            return randomChoice(
                "Well, that was undignified! My apologies for the clumsiness. " +
                "I'll get this cleaned up right away.",

                "That was... not my finest moment. Thank you for witnessing " +
                "my graceful failure with kindness. I'll address this now.",

                "I seem to have developed a talent for making mistakes. I'll " +
                "handle this and try to regain some semblance of competence."
            );
        }
    }

    /**
     * Generates player reassurance dialogue.
     */
    public static String generatePlayerReassurance(FailureContext context) {
        PersonalityTraits p = context.getPersonality();
        int failureCount = context.getPreviousFailureCount();

        if (p.getNeuroticism() >= 70) {
            return String.format(
                "I know you must be frustrated%s. I feel terrible about letting " +
                "you down. Your belief in me means so much - I don't want to " +
                "lose your trust. I'll work twice as hard to make this up to you!",
                failureCount > 0 ? " with my repeated mistakes" : ""
            );
        } else if (p.getAgreeableness() >= 70) {
            return String.format(
                "Your trust matters to me more than anything. I hate that I " +
                "damaged it%s. I want to earn back your faith, not just through " +
                "words but through actions. Please let me know what you need " +
                "from me to feel confident again.",
                failureCount > 0 ? " multiple times" : ""
            );
        } else if (p.getConscientiousness() >= 70) {
            return String.format(
                "I understand this failure has impacted your trust%s. I've " +
                "implemented corrective measures. I'll demonstrate improved " +
                "performance going forward.",
                failureCount > 0 ? " through repeated issues" : ""
            );
        } else {
            return String.format(
                "I know this is frustrating%s. I'm committed to making this " +
                "right and will work hard to regain your trust.",
                failureCount > 0 ? " - I've struggled with this" : ""
            );
        }
    }

    // Utility methods

    private static String randomChoice(String... options) {
        if (options == null || options.length == 0) {
            return "";
        }
        return options[ThreadLocalRandom.current().nextInt(options.length)];
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
