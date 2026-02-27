package com.minewright.personality;

/**
 * Demonstration of the TaskCompletionReporter system showing personality-appropriate
 * task completion reports across different specializations, complexities, and rapport levels.
 *
 * <p>This demo showcases:</p>
 * <ul>
 *   <li>All six specializations (Miner, Builder, Guard, Scout, Farmer, Artisan)</li>
 *   <li>Five task complexity tiers (Trivial to Epic)</li>
 *   <li>Four rapport stages (New Foreman to True Friend)</li>
 *   <li>Completion status variations (Full, Partial, Failure)</li>
 * </ul>
 *
 * @see TaskCompletionReporter
 * @since 1.4.0
 */
public class TaskCompletionDemo {

    public static void main(String[] args) {
        System.out.println("=".repeat(80));
        System.out.println("MINEWRIGHT TASK COMPLETION REPORTING SYSTEM - DEMO");
        System.out.println("=".repeat(80));
        System.out.println();

        // Demo 1: All Specializations - Simple Task, Low Rapport
        demoAllSpecializationsSimple();

        // Demo 2: Complexity Progression - Miner
        demoComplexityProgression();

        // Demo 3: Rapport Evolution - Builder
        demoRapportEvolution();

        // Demo 4: Epic Achievements - All Specializations
        demoEpicAchievements();

        // Demo 5: Partial Success Examples
        demoPartialSuccess();

        // Demo 6: Convenience Methods
        demoConvenienceMethods();

        System.out.println("=".repeat(80));
        System.out.println("DEMO COMPLETE");
        System.out.println("=".repeat(80));
    }

    private static void demoAllSpecializationsSimple() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("DEMO 1: ALL SPECIALIZATIONS - SIMPLE TASK, LOW RAPPORT");
        System.out.println("=".repeat(80));

        PersonalityTraits standardPersonality = PersonalityTraits.builder()
            .openness(60)
            .conscientiousness(70)
            .extraversion(50)
            .agreeableness(60)
            .neuroticism(40)
            .build();

        for (TaskCompletionReporter.Specialization spec : TaskCompletionReporter.Specialization.values()) {
            TaskCompletionReporter.CompletionReport report = TaskCompletionReporter.generateReport(
                TaskCompletionReporter.CompletionContext.builder()
                    .specialization(spec)
                    .personality(standardPersonality)
                    .rapport(15)
                    .taskType("simple task")
                    .complexity(TaskCompletionReporter.TaskComplexity.SIMPLE)
                    .build()
            );

            System.out.println("\n[" + spec.getTitle() + "]");
            System.out.println("Dialogue: " + report.getDialogue());
            System.out.println("Stage: " + report.getRapportStage().getRelationshipStyle());
            System.out.println("Follow-up: " + report.getSuggestedFollowUp());
        }
    }

    private static void demoComplexityProgression() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("DEMO 2: COMPLEXITY PROGRESSION - MINER");
        System.out.println("=".repeat(80));

        PersonalityTraits minerPersonality = PersonalityTraits.builder()
            .openness(50)
            .conscientiousness(90)
            .extraversion(40)
            .agreeableness(70)
            .neuroticism(40)
            .build();

        TaskCompletionReporter.TaskComplexity[] complexities = {
            TaskCompletionReporter.TaskComplexity.TRIVIAL,
            TaskCompletionReporter.TaskComplexity.SIMPLE,
            TaskCompletionReporter.TaskComplexity.MODERATE,
            TaskCompletionReporter.TaskComplexity.COMPLEX,
            TaskCompletionReporter.TaskComplexity.EPIC
        };

        for (TaskCompletionReporter.TaskComplexity complexity : complexities) {
            TaskCompletionReporter.CompletionReport report = TaskCompletionReporter.generateReport(
                TaskCompletionReporter.CompletionContext.builder()
                    .specialization(TaskCompletionReporter.Specialization.MINER)
                    .personality(minerPersonality)
                    .rapport(40)
                    .taskType("mining")
                    .complexity(complexity)
                    .addResult("count", complexity.getTier() * 5)
                    .addResult("resource", "iron ore")
                    .build()
            );

            System.out.println("\n[" + complexity.getDisplayName() + " - Tier " + complexity.getTier() + "]");
            System.out.println("Dialogue: " + report.getDialogue());
        }
    }

    private static void demoRapportEvolution() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("DEMO 3: RAPPORT EVOLUTION - BUILDER");
        System.out.println("=".repeat(80));

        PersonalityTraits builderPersonality = PersonalityTraits.builder()
            .openness(80)
            .conscientiousness(95)
            .extraversion(60)
            .agreeableness(50)
            .neuroticism(60)
            .build();

        int[] rapportLevels = {10, 35, 60, 90};
        String[] stageNames = {"New Foreman", "Reliable Worker", "Trusted Partner", "True Friend"};

        for (int i = 0; i < rapportLevels.length; i++) {
            TaskCompletionReporter.CompletionReport report = TaskCompletionReporter.generateReport(
                TaskCompletionReporter.CompletionContext.builder()
                    .specialization(TaskCompletionReporter.Specialization.BUILDER)
                    .personality(builderPersonality)
                    .rapport(rapportLevels[i])
                    .taskType("construction")
                    .complexity(TaskCompletionReporter.TaskComplexity.MODERATE)
                    .addResult("structure", "library wing")
                    .playerName("Alex")
                    .build()
            );

            System.out.println("\n[" + stageNames[i] + " - Rapport: " + rapportLevels[i] + "]");
            System.out.println("Dialogue: " + report.getDialogue());
            System.out.println("Stage: " + report.getRapportStage().getRelationshipStyle());
        }
    }

    private static void demoEpicAchievements() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("DEMO 4: EPIC ACHIEVEMENTS - ALL SPECIALIZATIONS");
        System.out.println("=".repeat(80));

        for (TaskCompletionReporter.Specialization spec : TaskCompletionReporter.Specialization.values()) {
            PersonalityTraits personality = getPersonalityForSpecialization(spec);

            TaskCompletionReporter.CompletionReport report = TaskCompletionReporter.generateReport(
                TaskCompletionReporter.CompletionContext.builder()
                    .specialization(spec)
                    .personality(personality)
                    .rapport(85)
                    .taskType("epic achievement")
                    .complexity(TaskCompletionReporter.TaskComplexity.EPIC)
                    .playerName("Alex")
                    .addResult("count", 47)
                    .build()
            );

            System.out.println("\n[" + spec.getTitle() + "]");
            System.out.println("Dialogue: " + report.getDialogue());
            System.out.println("Stage: " + report.getRapportStage().getRelationshipStyle());
            System.out.println("Includes Humor: " + report.includesHumor());
        }
    }

    private static void demoPartialSuccess() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("DEMO 5: PARTIAL SUCCESS EXAMPLES");
        System.out.println("=".repeat(80));

        TaskCompletionReporter.Specialization[] specializations = {
            TaskCompletionReporter.Specialization.MINER,
            TaskCompletionReporter.Specialization.BUILDER,
            TaskCompletionReporter.Specialization.SCOUT
        };

        for (TaskCompletionReporter.Specialization spec : specializations) {
            PersonalityTraits personality = getPersonalityForSpecialization(spec);

            TaskCompletionReporter.CompletionReport report = TaskCompletionReporter.generateReport(
                TaskCompletionReporter.CompletionContext.builder()
                    .specialization(spec)
                    .personality(personality)
                    .rapport(50)
                    .taskType("partial task")
                    .complexity(TaskCompletionReporter.TaskComplexity.MODERATE)
                    .completionPercentage(65)
                    .addResult("count", 18)
                    .addResult("obstacle", "unexpected complication")
                    .playerName("Alex")
                    .build()
            );

            System.out.println("\n[" + spec.getTitle() + " - 65% Complete]");
            System.out.println("Dialogue: " + report.getDialogue());
            System.out.println("Status: " + report.getStatus().getDisplayName());
        }
    }

    private static void demoConvenienceMethods() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("DEMO 6: CONVENIENCE METHODS");
        System.out.println("=".repeat(80));

        PersonalityTraits personality = PersonalityTraits.builder()
            .openness(70)
            .conscientiousness(80)
            .extraversion(60)
            .agreeableness(60)
            .neuroticism(40)
            .build();

        // Mining report
        System.out.println("\n[Mining Report]");
        TaskCompletionReporter.CompletionReport miningReport =
            TaskCompletionReporter.miningReport(personality, 60, 27, "diamond ore");
        System.out.println(miningReport.getDialogue());

        // Construction report
        System.out.println("\n[Construction Report]");
        TaskCompletionReporter.CompletionReport constructionReport =
            TaskCompletionReporter.constructionReport(personality, 70, "observatory tower");
        System.out.println(constructionReport.getDialogue());

        // Combat report
        System.out.println("\n[Combat Report]");
        TaskCompletionReporter.CompletionReport combatReport =
            TaskCompletionReporter.combatReport(personality, 55, 8);
        System.out.println(combatReport.getDialogue());
    }

    private static PersonalityTraits getPersonalityForSpecialization(
            TaskCompletionReporter.Specialization spec) {
        return switch (spec) {
            case MINER -> PersonalityTraits.builder()
                .openness(50).conscientiousness(90).extraversion(40)
                .agreeableness(70).neuroticism(40).build();
            case BUILDER -> PersonalityTraits.builder()
                .openness(80).conscientiousness(95).extraversion(60)
                .agreeableness(50).neuroticism(60).build();
            case GUARD -> PersonalityTraits.builder()
                .openness(40).conscientiousness(80).extraversion(70)
                .agreeableness(40).neuroticism(30).build();
            case SCOUT -> PersonalityTraits.builder()
                .openness(95).conscientiousness(50).extraversion(70)
                .agreeableness(80).neuroticism(20).build();
            case FARMER -> PersonalityTraits.builder()
                .openness(60).conscientiousness(85).extraversion(50)
                .agreeableness(90).neuroticism(30).build();
            case ARTISAN -> PersonalityTraits.builder()
                .openness(90).conscientiousness(80).extraversion(40)
                .agreeableness(60).neuroticism(50).build();
        };
    }
}
