package com.minewright.personality;

/**
 * Demonstration of the FailureResponseGenerator system.
 *
 * <p>This class shows how different personalities respond to various types
 * and severities of failures, creating emotionally intelligent and
 * character-appropriate dialogue.</p>
 *
 * @since 1.3.0
 */
public class FailureResponseDemo {

    public static void main(String[] args) {
        System.out.println("=== MineWright Failure Response System Demo ===\n");

        // Create different personality types
        PersonalityTraits perfectionist = PersonalityTraits.builder()
                .openness(40)
                .conscientiousness(95)
                .extraversion(30)
                .agreeableness(50)
                .neuroticism(30)
                .build();

        PersonalityTraits worrier = PersonalityTraits.builder()
                .openness(40)
                .conscientiousness(60)
                .extraversion(50)
                .agreeableness(80)
                .neuroticism(95)
                .build();

        PersonalityTraits stoic = PersonalityTraits.builder()
                .openness(40)
                .conscientiousness(60)
                .extraversion(30)
                .agreeableness(40)
                .neuroticism(5)
                .build();

        PersonalityTraits enthusiastic = PersonalityTraits.builder()
                .openness(70)
                .conscientiousness(50)
                .extraversion(95)
                .agreeableness(70)
                .neuroticism(40)
                .build();

        PersonalityTraits balanced = PersonalityTraits.builder()
                .openness(50)
                .conscientiousness(50)
                .extraversion(50)
                .agreeableness(50)
                .neuroticism(50)
                .build();

        // Demo 1: Lost Diamond Pickaxe (Major failure)
        System.out.println("=== SCENARIO 1: Lost Diamond Pickaxe (Severity: 65) ===\n");
        demoResponse(perfectionist, FailureResponseGenerator.FailureType.ITEM_LOSS, 65, "Perfectionist");
        demoResponse(worrier, FailureResponseGenerator.FailureType.ITEM_LOSS, 65, "Worrier");
        demoResponse(stoic, FailureResponseGenerator.FailureType.ITEM_LOSS, 65, "Stoic");
        demoResponse(enthusiastic, FailureResponseGenerator.FailureType.ITEM_LOSS, 65, "Enthusiastic");

        // Demo 2: Minor navigation error
        System.out.println("\n=== SCENARIO 2: Took Wrong Path (Severity: 15) ===\n");
        demoResponse(perfectionist, FailureResponseGenerator.FailureType.NAVIGATION_ERROR, 15, "Perfectionist");
        demoResponse(worrier, FailureResponseGenerator.FailureType.NAVIGATION_ERROR, 15, "Worrier");
        demoResponse(enthusiastic, FailureResponseGenerator.FailureType.NAVIGATION_ERROR, 15, "Enthusiastic");

        // Demo 3: Structural collapse (Critical failure)
        System.out.println("\n=== SCENARIO 3: Building Collapsed (Severity: 85) ===\n");
        demoResponse(balanced, FailureResponseGenerator.FailureType.STRUCTURAL_FAILURE, 85, "Balanced");
        demoResponse(perfectionist, FailureResponseGenerator.FailureType.STRUCTURAL_FAILURE, 85, "Perfectionist");

        // Demo 4: Repeated failure
        System.out.println("\n=== SCENARIO 4: Repeated Tool Breakage (Severity: 35, 3rd time) ===\n");
        FailureResponseGenerator.FailureContext context = FailureResponseGenerator.FailureContext.builder()
                .failureType(FailureResponseGenerator.FailureType.TOOL_BREAKAGE)
                .severity(35)
                .personality(balanced)
                .previousFailureCount(3)
                .build();
        FailureResponseGenerator.FailureResponse response = FailureResponseGenerator.generateResponse(context);
        System.out.println("Personality: Balanced");
        System.out.println("Dialogue: " + response.getDialogue());
        System.out.println("Learning: " + response.getLearningStatement());
        System.out.println("Recovery: " + response.getRecoveryPlan());

        // Demo 5: Help request
        System.out.println("\n=== SCENARIO 5: Requesting Help ===\n");
        System.out.println("Perfectionist: " + FailureResponseGenerator.generateHelpRequest(
                FailureResponseGenerator.FailureContext.builder()
                        .failureType(FailureResponseGenerator.FailureType.COMMUNICATION_ERROR)
                        .severity(50)
                        .personality(perfectionist)
                        .build()
        ));
        System.out.println("\nWorrier: " + FailureResponseGenerator.generateHelpRequest(
                FailureResponseGenerator.FailureContext.builder()
                        .failureType(FailureResponseGenerator.FailureType.COMMUNICATION_ERROR)
                        .severity(50)
                        .personality(worrier)
                        .build()
        ));
        System.out.println("\nEnthusiastic: " + FailureResponseGenerator.generateHelpRequest(
                FailureResponseGenerator.FailureContext.builder()
                        .failureType(FailureResponseGenerator.FailureType.COMMUNICATION_ERROR)
                        .severity(50)
                        .personality(enthusiastic)
                        .build()
        ));

        // Demo 6: Embarrassing moment
        System.out.println("\n=== SCENARIO 6: Embarrassing Failure (Got Stuck) ===\n");
        System.out.println("Enthusiastic: " + FailureResponseGenerator.generateEmbarrassmentResponse(
                FailureResponseGenerator.FailureContext.builder()
                        .failureType(FailureResponseGenerator.FailureType.EMBARRASSING_MOMENT)
                        .severity(25)
                        .personality(enthusiastic)
                        .build()
        ));
        System.out.println("\nStoic: " + FailureResponseGenerator.generateEmbarrassmentResponse(
                FailureResponseGenerator.FailureContext.builder()
                        .failureType(FailureResponseGenerator.FailureType.EMBARRASSING_MOMENT)
                        .severity(25)
                        .personality(stoic)
                        .build()
        ));
        System.out.println("\nWorrier: " + FailureResponseGenerator.generateEmbarrassmentResponse(
                FailureResponseGenerator.FailureContext.builder()
                        .failureType(FailureResponseGenerator.FailureType.EMBARRASSING_MOMENT)
                        .severity(25)
                        .personality(worrier)
                        .build()
        ));

        // Demo 7: Player reassurance
        System.out.println("\n=== SCENARIO 7: Reassuring Player After Failure ===\n");
        System.out.println("Worrier: " + FailureResponseGenerator.generatePlayerReassurance(
                FailureResponseGenerator.FailureContext.builder()
                        .failureType(FailureResponseGenerator.FailureType.RESOURCE_WASTE)
                        .severity(70)
                        .personality(worrier)
                        .previousFailureCount(2)
                        .build()
        ));
        System.out.println("\nPerfectionist: " + FailureResponseGenerator.generatePlayerReassurance(
                FailureResponseGenerator.FailureContext.builder()
                        .failureType(FailureResponseGenerator.FailureType.RESOURCE_WASTE)
                        .severity(70)
                        .personality(perfectionist)
                        .previousFailureCount(2)
                        .build()
        ));

        System.out.println("\n=== Demo Complete ===");
    }

    private static void demoResponse(PersonalityTraits personality,
                                    FailureResponseGenerator.FailureType type,
                                    int severity,
                                    String personalityName) {
        FailureResponseGenerator.FailureContext context = FailureResponseGenerator.createQuickContext(type, severity, personality);
        FailureResponseGenerator.FailureResponse response = FailureResponseGenerator.generateResponse(context);

        System.out.println("Personality: " + personalityName);
        System.out.println("Dialogue: " + response.getDialogue());
        if (!response.getLearningStatement().isEmpty()) {
            System.out.println("Learning: " + response.getLearningStatement());
        }
        if (!response.getRecoveryPlan().isEmpty()) {
            System.out.println("Recovery: " + response.getRecoveryPlan());
        }
        if (response.needsPlayerReassurance()) {
            System.out.println("[Player reassurance recommended]");
        }
        System.out.println();
    }
}
