package com.minewright.personality;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Generates personality-appropriate task completion reports for MineWright workers.
 *
 * <p>This system combines:</p>
 * <ul>
 *   <li>Task complexity classification (1-5 tiers)</li>
 *   <li>Completion status (full success, partial success, failure)</li>
 *   <li>OCEAN personality trait analysis</li>
 *   <li>Rapport-based relationship evolution</li>
 *   <li>Specialization-specific voice patterns</li>
 * </ul>
 *
 * <p>Research basis:</p>
 * <ul>
 *   <li>Workplace closed-loop communication psychology</li>
 *   <li>Skyrim/Fallout NPC quest completion systems</li>
 *   <li>Personality-driven communication styles (DiSC, Merrill-Reid)</li>
 * </ul>
 *
 * @see PersonalityTraits
 * @see FailureResponseGenerator
 * @since 1.4.0
 */
public class TaskCompletionReporter {

    /**
     * Represents the five task complexity tiers.
     */
    public enum TaskComplexity {
        TRIVIAL(1, 0, 20, "Trivial"),
        SIMPLE(2, 21, 40, "Simple"),
        MODERATE(3, 41, 60, "Moderate"),
        COMPLEX(4, 61, 80, "Complex"),
        EPIC(5, 81, 100, "Epic");

        private final int tier;
        private final int minComplexity;
        private final int maxComplexity;
        private final String displayName;

        TaskComplexity(int tier, int minComplexity, int maxComplexity, String displayName) {
            this.tier = tier;
            this.minComplexity = minComplexity;
            this.maxComplexity = maxComplexity;
            this.displayName = displayName;
        }

        public static TaskComplexity fromComplexity(int complexity) {
            for (TaskComplexity tier : values()) {
                if (complexity >= tier.minComplexity && complexity <= tier.maxComplexity) {
                    return tier;
                }
            }
            return EPIC;
        }

        public int getTier() { return tier; }
        public String getDisplayName() { return displayName; }
    }

    /**
     * Represents the completion status of a task.
     */
    public enum CompletionStatus {
        FULL_SUCCESS(100, "Full Success"),
        PARTIAL_SUCCESS(50, 99, "Partial Success"),
        FAILURE(0, 49, "Failure");

        private final int minCompletion;
        private final int maxCompletion;
        private final String displayName;

        CompletionStatus(int minCompletion, int maxCompletion, String displayName) {
            this.minCompletion = minCompletion;
            this.maxCompletion = maxCompletion;
            this.displayName = displayName;
        }

        CompletionStatus(int exactCompletion, String displayName) {
            this.minCompletion = exactCompletion;
            this.maxCompletion = exactCompletion;
            this.displayName = displayName;
        }

        public static CompletionStatus fromCompletion(int completion) {
            if (completion == 100) return FULL_SUCCESS;
            if (completion >= 50) return PARTIAL_SUCCESS;
            return FAILURE;
        }

        public String getDisplayName() { return displayName; }
    }

    /**
     * Represents the six worker specializations.
     */
    public enum Specialization {
        MINER("The Excavator"),
        BUILDER("The Architect"),
        GUARD("The Protector"),
        SCOUT("The Pathfinder"),
        FARMER("The Cultivator"),
        ARTISAN("The Crafter");

        private final String title;

        Specialization(String title) {
            this.title = title;
        }

        public String getTitle() { return title; }
    }

    /**
     * Represents rapport-based relationship stages.
     */
    public enum RapportStage {
        NEW_FOREMAN(0, 25, "Sir", "Professional"),
        RELIABLE_WORKER(26, 50, "Boss", "Collaborative"),
        TRUSTED_PARTNER(51, 75, "Friend", "Friendly"),
        TRUE_FRIEND(76, 100, "Best Friend", "Intimate");

        private final int minRapport;
        private final int maxRapport;
        private final String defaultAddress;
        private final String relationshipStyle;

        RapportStage(int minRapport, int maxRapport, String defaultAddress, String relationshipStyle) {
            this.minRapport = minRapport;
            this.maxRapport = maxRapport;
            this.defaultAddress = defaultAddress;
            this.relationshipStyle = relationshipStyle;
        }

        public static RapportStage fromRapport(int rapport) {
            for (RapportStage stage : values()) {
                if (rapport >= stage.minRapport && rapport <= stage.maxRapport) {
                    return stage;
                }
            }
            return NEW_FOREMAN;
        }

        public String getDefaultAddress() { return defaultAddress; }
        public String getRelationshipStyle() { return relationshipStyle; }
    }

    /**
     * Context for generating task completion reports.
     */
    public static class CompletionContext {
        private final Specialization specialization;
        private final PersonalityTraits personality;
        private final int rapport;
        private final TaskComplexity complexity;
        private final CompletionStatus status;
        private final int completionPercentage;
        private final String taskType;
        private final Map<String, Object> results;
        private final String playerName;

        private CompletionContext(Builder builder) {
            this.specialization = builder.specialization;
            this.personality = builder.personality;
            this.rapport = builder.rapport;
            this.complexity = builder.complexity;
            this.status = builder.status;
            this.completionPercentage = builder.completionPercentage;
            this.taskType = builder.taskType;
            this.results = Collections.unmodifiableMap(builder.results);
            this.playerName = builder.playerName;
        }

        public Specialization getSpecialization() { return specialization; }
        public PersonalityTraits getPersonality() { return personality; }
        public int getRapport() { return rapport; }
        public TaskComplexity getComplexity() { return complexity; }
        public CompletionStatus getStatus() { return status; }
        public int getCompletionPercentage() { return completionPercentage; }
        public String getTaskType() { return taskType; }
        public Map<String, Object> getResults() { return results; }
        public String getPlayerName() { return playerName; }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private Specialization specialization;
            private PersonalityTraits personality;
            private Integer rapport = 0;
            private TaskComplexity complexity = TaskComplexity.SIMPLE;
            private CompletionStatus status = CompletionStatus.FULL_SUCCESS;
            private Integer completionPercentage = 100;
            private String taskType = "task";
            private Map<String, Object> results = new HashMap<>();
            private String playerName = "Player";

            public Builder specialization(Specialization specialization) {
                this.specialization = specialization;
                return this;
            }

            public Builder personality(PersonalityTraits personality) {
                this.personality = personality;
                return this;
            }

            public Builder rapport(int rapport) {
                this.rapport = rapport;
                return this;
            }

            public Builder complexity(TaskComplexity complexity) {
                this.complexity = complexity;
                return this;
            }

            public Builder complexity(int complexityValue) {
                this.complexity = TaskComplexity.fromComplexity(complexityValue);
                return this;
            }

            public Builder status(CompletionStatus status) {
                this.status = status;
                return this;
            }

            public Builder completionPercentage(int percentage) {
                this.completionPercentage = percentage;
                this.status = CompletionStatus.fromCompletion(percentage);
                return this;
            }

            public Builder taskType(String taskType) {
                this.taskType = taskType;
                return this;
            }

            public Builder addResult(String key, Object value) {
                this.results.put(key, value);
                return this;
            }

            public Builder playerName(String playerName) {
                this.playerName = playerName;
                return this;
            }

            public CompletionContext build() {
                if (specialization == null) {
                    throw new IllegalStateException("specialization is required");
                }
                if (personality == null) {
                    throw new IllegalStateException("personality is required");
                }
                return new CompletionContext(this);
            }
        }
    }

    /**
     * Represents a generated completion report.
     */
    public static class CompletionReport {
        private final String dialogue;
        private final RapportStage rapportStage;
        private final TaskComplexity complexity;
        private final CompletionStatus status;
        private final boolean includesHumor;
        private final String suggestedFollowUp;

        public CompletionReport(String dialogue, RapportStage rapportStage,
                              TaskComplexity complexity, CompletionStatus status,
                              boolean includesHumor, String suggestedFollowUp) {
            this.dialogue = dialogue;
            this.rapportStage = rapportStage;
            this.complexity = complexity;
            this.status = status;
            this.includesHumor = includesHumor;
            this.suggestedFollowUp = suggestedFollowUp;
        }

        public String getDialogue() { return dialogue; }
        public RapportStage getRapportStage() { return rapportStage; }
        public TaskComplexity getComplexity() { return complexity; }
        public CompletionStatus getStatus() { return status; }
        public boolean includesHumor() { return includesHumor; }
        public String getSuggestedFollowUp() { return suggestedFollowUp; }

        @Override
        public String toString() {
            return dialogue;
        }
    }

    // ============== DIALOGUE TEMPLATES ==============

    private static final Map<Specialization, List<String>> TRIVIAL_SUCCESS_TEMPLATES = Map.of(
        Specialization.MINER, List.of(
            "Done.",
            "Got it.",
            "Cleared.",
            "Stone removed."
        ),
        Specialization.BUILDER, List.of(
            "Placed.",
            "Done.",
            "Aligned.",
            "Block set."
        ),
        Specialization.GUARD, List.of(
            "Secured.",
            "Done.",
            "Clear.",
            "Area safe."
        ),
        Specialization.SCOUT, List.of(
            "Found it!",
            "Waypoint reached!",
            "Done!",
            "Got it!"
        ),
        Specialization.FARMER, List.of(
            "Done.",
            "Tended.",
            "Planted.",
            "Harvested."
        ),
        Specialization.ARTISAN, List.of(
            "Crafted.",
            "Done.",
            "Complete.",
            "Ratios correct."
        )
    );

    private static final Map<Specialization, List<String>> SIMPLE_SUCCESS_TEMPLATES = Map.of(
        Specialization.MINER, List.of(
            "Ore extracted. {count} {resource} collected.",
            "Vein cleared. Got {count}. Not bad.",
            "Mining done. {count} units.",
            "Stone cleared. {count} {resource}."
        ),
        Specialization.BUILDER, List.of(
            "Foundation laid. Ready for next phase.",
            "Walls erected. Square within tolerance.",
            "Structure complete. Quality maintained.",
            "Placement done. Symmetry verified."
        ),
        Specialization.GUARD, List.of(
            "Hostile neutralized. Perimeter secure.",
            "Cave checked. Clear of threats.",
            "Patrol complete. Area safe.",
            "{count} hostiles eliminated. We're good."
        ),
        Specialization.SCOUT, List.of(
            "Location mapped! Found {discovery}.",
            "Discovered {discovery}! Want to see?",
            "Area explored! {discovery} located!",
            "Scouting done! Found something interesting!"
        ),
        Specialization.FARMER, List.of(
            "Crops tended. Growth rate optimal.",
            "Harvest gathered. {count} units collected.",
            "Soil prepared. Ready for planting.",
            "Plants watered. Nature provides."
        ),
        Specialization.ARTISAN, List.of(
            "Items crafted. {count} complete.",
            "Smelting done. Efficiency at {efficiency}%.",
            "Recipe complete. Ratios optimal.",
            "Crafting finished. {count} units ready."
        )
    );

    private static final Map<Specialization, List<String>> MODERATE_SUCCESS_TEMPLATES = Map.of(
        Specialization.MINER, List.of(
            "Excavation complete. Vein yielded {count} {resource}. Excellent yield.",
            "Mining done. {count} units of {resource}. This is why we dig.",
            "Vein exhausted. {count} collected. Not a bad haul.",
            "Extraction complete. Found {count} {resource}. Solid work."
        ),
        Specialization.BUILDER, List.of(
            "{structure} complete. Symmetry verified. Structural integrity confirmed.",
            "Construction finished. Every angle perfect. Quality matters.",
            "Build done. Took {time}. Proper work from start to finish.",
            "{structure} erected. Checked the alignment - flawless."
        ),
        Specialization.GUARD, List.of(
            "Cave cleared. {count} hostiles eliminated. No casualties. Mission accomplished.",
            "Area secured. All threats neutralized. Perimeter is safe.",
            "Clearance complete. {count} enemies down. Nothing getting through.",
            "Zone pacified. {count} hostiles dealt with. We're good."
        ),
        Specialization.SCOUT, List.of(
            "Cave system mapped! Discovered {discovery}! It's incredible!",
            "Exploration complete! Found {discovery}! You have to see this!",
            "Area charted! {discovery} located! Never seen anything like it!",
            "Mapping done! {discovery} discovered! Adventure awaits!"
        ),
        Specialization.FARMER, List.of(
            "Harvest complete. {count} units collected. The crops grew strong.",
            "Farming done. Nature blessed us with a good yield.",
            "Crops gathered. {count} units. Grateful for the harvest.",
            "Season complete. The plants provided well."
        ),
        Specialization.ARTISAN, List.of(
            "Crafting batch complete. {count} items. Efficiency within optimal range.",
            "Recipe executed. {count} units. Ratios were perfect.",
            "Production done. {count} items. Technical precision achieved.",
            "Manufacturing complete. {count} ready. The math worked out beautifully."
        )
    );

    private static final Map<Specialization, List<String>> COMPLEX_SUCCESS_TEMPLATES = Map.of(
        Specialization.MINER, List.of(
            "Mining expedition concluded. Reached {depth}. Collected {count} diamonds. Significant haul.",
            "Deep excavation complete. {count} {resource} from the motherlode. This is good stone.",
            "Major vein tapped. {count} units. This is why we go down deep.",
            "Extended mining done. {count} {resource} collected. NOW we're talking."
        ),
        Specialization.BUILDER, List.of(
            "{structure} complete. Three stories, finished details. This was satisfying to build.",
            "Major construction done. Every block intentional. Architecture at its finest.",
            "{structure} finished. Took {time}. Quality from foundation to roof.",
            "Building complete. The symmetry... perfection. This is proper construction."
        ),
        Specialization.GUARD, List.of(
            "Monument cleared. {count} hostiles eliminated. Crew safe. That's what matters.",
            "Major threat neutralized. Area secured. Nobody died on my watch.",
            "Defensive operation complete. Perimeter established. We're protected.",
            "Large-scale clearance done. {count} enemies. Mission accomplished."
        ),
        Specialization.SCOUT, List.of(
            "Major discovery! Found {discovery}! This is incredible! You HAVE to see this!",
            "Epic exploration complete! Discovered {discovery}! Best adventure ever!",
            "Mapping expedition done! {discovery} found! This changes everything!",
            "Massive cave charted! {discovery} located! I can't believe what I found!"
        ),
        Specialization.FARMER, List.of(
            "Farm complete. All crops planted, automated systems ready. Nature will provide.",
            "Agricultural project done. {count} plants in the ground. In time, a harvest.",
            "Growing operation established. Everything planted. Now we wait and tend.",
            "Farm finished. The soil is ready. Life will grow here."
        ),
        Specialization.ARTISAN, List.of(
            "Redstone system complete. Circuit efficiency at {efficiency}%. Elegant design.",
            "Major crafting project done. {count} items. Technical perfection achieved.",
            "Production line operational. {count} units. The ratios are beautiful.",
            "Complex recipe executed. {count} finished. This is the pinnacle of crafting."
        )
    );

    private static final Map<Specialization, List<String>> EPIC_SUCCESS_TEMPLATES = Map.of(
        Specialization.MINER, List.of(
            "This expedition... down deep where we belong... found the motherlode. {count} diamonds from one cave. I've been mining twenty years and never seen anything like this.",
            "We... we actually did it. Reached the deepest point. {count} diamonds. This is the greatest find of my career. I'll never forget this expedition.",
            "The motherlode. {count} diamonds. {count} emeralds. This cave... it's special. Good stone, good vibes. I'd like to come back. With you."
        ),
        Specialization.BUILDER, List.of(
            "It's magnificent. {time} of planning and building. Look at those arches. That roofline. The symmetry brings tears to my eyes. This is what construction was meant to be.",
            "This castle... every block placed by hand. Every tower perfect. We built this together. This might be my favorite build ever.",
            "Masterpiece doesn't begin to cover it. {structure} is finished. The perfection... the artistry... I've never built anything like this."
        ),
        Specialization.GUARD, List.of(
            "Monument cleared. I don't say this lightly - that was the hardest fight of my life. {count} hostiles. But the crew is alive. Everyone made it back. That's the only victory statistic that matters.",
            "We... we survived. The fortress is cleared. {count} enemies. I thought we'd lose people. But we made it. I'm proud of us. I don't say that lightly."
        ),
        Specialization.SCOUT, List.of(
            "I found... I found paradise. {discovery}. Massive cavern, underground lake, glowstone ceiling. It's beautiful. I cried a little. Can we build a base there? Please?",
            "This is it. The greatest discovery of my career. {discovery}. I've explored everywhere and never seen anything like this. This is why I'm a scout."
        ),
        Specialization.FARMER, List.of(
            "The farm is complete. Every crop, every automated system, every corner of soil nurtured. When I tend these plants, I feel connected to something ancient. We're not just growing food. We're growing life.",
            "This harvest... {count} units. The best yield I've ever seen. Nature blessed us. I'm grateful for every plant, every seed, every moment in the soil."
        ),
        Specialization.ARTISAN, List.of(
            "The automated system is finished. Redstone logic, hopper timing, villager optimization - every system integrated. Efficiency at {efficiency}%. This is technical perfection.",
            "This project... the calculations, the ratios, the redstone... it's all come together. {count} items of perfect quality. This is the pinnacle of crafting. I've worked toward this my entire career."
        )
    );

    // ============== MAIN GENERATION METHOD ==============

    /**
     * Generates a personality-appropriate task completion report.
     *
     * @param context The completion context
     * @return A completion report with dialogue and metadata
     */
    public static CompletionReport generateReport(CompletionContext context) {
        RapportStage rapportStage = RapportStage.fromRapport(context.getRapport());
        String baseDialogue = selectBaseTemplate(context);
        String personalizedDialogue = applyPersonality(baseDialogue, context);
        String rapportAdjustedDialogue = adjustForRapport(personalizedDialogue, context, rapportStage);
        String finalDialogue = injectResults(rapportAdjustedDialogue, context);
        boolean includesHumor = shouldIncludeHumor(context);
        String suggestedFollowUp = generateFollowUp(context);

        return new CompletionReport(
            finalDialogue,
            rapportStage,
            context.getComplexity(),
            context.getStatus(),
            includesHumor,
            suggestedFollowUp
        );
    }

    private static String selectBaseTemplate(CompletionContext context) {
        if (context.getStatus() == CompletionStatus.FAILURE) {
            return selectFailureTemplate(context);
        } else if (context.getStatus() == CompletionStatus.PARTIAL_SUCCESS) {
            return selectPartialTemplate(context);
        }

        // Full success templates
        List<String> templates = switch (context.getComplexity()) {
            case TRIVIAL -> TRIVIAL_SUCCESS_TEMPLATES.get(context.getSpecialization());
            case SIMPLE -> SIMPLE_SUCCESS_TEMPLATES.get(context.getSpecialization());
            case MODERATE -> MODERATE_SUCCESS_TEMPLATES.get(context.getSpecialization());
            case COMPLEX -> COMPLEX_SUCCESS_TEMPLATES.get(context.getSpecialization());
            case EPIC -> EPIC_SUCCESS_TEMPLATES.get(context.getSpecialization());
        };

        if (templates == null || templates.isEmpty()) {
            return "Task complete.";
        }

        return templates.get(ThreadLocalRandom.current().nextInt(templates.size()));
    }

    private static String selectFailureTemplate(CompletionContext context) {
        // Delegate to FailureResponseGenerator for failure cases
        // For now, return a simple failure message
        return "Task failed. I apologize for the inconvenience.";
    }

    private static String selectPartialTemplate(CompletionContext context) {
        Specialization spec = context.getSpecialization();
        List<String> templates = switch (spec) {
            case MINER -> List.of(
                "Got most of the {resource}. Hit {obstacle}. Lost the rest. Can go back.",
                "Mining partially done. {count} units collected. {obstacle} stopped me.",
                "Vein mostly cleared. {count} collected. Need to get the rest."
            );
            case BUILDER -> List.of(
                "{structure} mostly done. {obstacle}. Need {requirement} to finish.",
                "Construction partially complete. {obstacle}. Working on solution.",
                "Build is at {percentage}%. {obstacle}. Need to adjust plans."
            );
            case GUARD -> List.of(
                "Area mostly cleared. {obstacle}. Might need backup.",
                "Threats reduced. {obstacle}. Need reinforcements.",
                "Cave partially secure. {obstacle}. Recommending assistance."
            );
            case SCOUT -> List.of(
                "Found {discovery}! Didn't map everything. Got excited. Want more?",
                "Exploration partially done. {obstacle}. Still so much to see!",
                "Located {discovery}. {obstacle}. Can continue if you want."
            );
            case FARMER -> List.of(
                "Crops planted, but {obstacle}. Growth will be slower.",
                "Farming partially done. {obstacle}. Nature can't be rushed.",
                "Harvest gathered. {obstacle}. Still got {count} units."
            );
            case ARTISAN -> List.of(
                "Crafting mostly done. Short {requirement}. Recipe was unexpected.",
                "Production partial. {obstacle}. Adjusting calculations.",
                "{count} items made. {obstacle}. Need to fix the ratios."
            );
        };

        return templates.get(ThreadLocalRandom.current().nextInt(templates.size()));
    }

    private static String applyPersonality(String dialogue, CompletionContext context) {
        // Adjust dialogue based on personality traits
        PersonalityTraits p = context.getPersonality();

        // High extraversion = more exclamation marks and enthusiasm
        if (p.getExtraversion() > 70) {
            dialogue = dialogue.replace(".", "!").replace("!!", "!");
            if (!dialogue.contains("!")) {
                dialogue += "!";
            }
        }

        // High neuroticism = more tentative language for non-success
        if (p.getNeuroticism() > 70 && context.getStatus() != CompletionStatus.FULL_SUCCESS) {
            if (!dialogue.toLowerCase().contains("sorry") &&
                !dialogue.toLowerCase().contains("apologize")) {
                dialogue = "I... " + dialogue.substring(0, 1).toLowerCase() + dialogue.substring(1);
            }
        }

        // High conscientiousness = more detail and precision
        if (p.getConscientiousness() > 80) {
            // Add precision to numbers if present
            dialogue = dialogue.replaceAll("(\\d+)(?!.*\\d)", "$1 units");
        }

        return dialogue;
    }

    private static String adjustForRapport(String dialogue, CompletionContext context,
                                          RapportStage stage) {
        String playerName = context.getPlayerName();
        String address = stage.getDefaultAddress();

        // Adjust address based on specialization and rapport
        if (stage == RapportStage.RELIABLE_WORKER) {
            address = switch (context.getSpecialization()) {
                case MINER -> "Chief";
                case BUILDER -> "Boss";
                case GUARD -> "Captain";
                case SCOUT, FARMER -> playerName;
                case ARTISAN -> "Engineer";
            };
        } else if (stage == RapportStage.TRUSTED_PARTNER ||
                   stage == RapportStage.TRUE_FRIEND) {
            address = playerName;
        }

        // Add address to beginning of dialogue
        if (stage != RapportStage.NEW_FOREMAN &&
            context.getComplexity() != TaskComplexity.TRIVIAL) {
            dialogue = address + ", " + dialogue.substring(0, 1).toLowerCase() +
                      dialogue.substring(1);
        }

        // High rapport = more personal/emotional
        if (stage == RapportStage.TRUE_FRIEND &&
            context.getComplexity() == TaskComplexity.EPIC) {
            dialogue = dialogue.replace(" I", " we").replace(" my", " our");
        }

        return dialogue;
    }

    private static String injectResults(String dialogue, CompletionContext context) {
        Map<String, Object> results = context.getResults();

        // Replace placeholders with actual results
        for (Map.Entry<String, Object> entry : results.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "unknown";
            dialogue = dialogue.replace(placeholder, value);
        }

        // Fill in missing placeholders with defaults
        dialogue = dialogue.replace("{count}", "several");
        dialogue = dialogue.replace("{resource}", "materials");
        dialogue = dialogue.replace("{structure}", "structure");
        dialogue = dialogue.replace("{discovery}", "something interesting");
        dialogue = dialogue.replace("{obstacle}", "ran into a complication");
        dialogue = dialogue.replace("{requirement}", "more materials");
        dialogue = dialogue.replace("{percentage}", context.getCompletionPercentage() + "%");
        dialogue = dialogue.replace("{efficiency}", "optimal");
        dialogue = dialogue.replace("{depth}", "the target depth");
        dialogue = dialogue.replace("{time}", "a while");

        return dialogue;
    }

    private static boolean shouldIncludeHumor(CompletionContext context) {
        // Humor only for full success, medium+ rapport, not trivial tasks
        if (context.getStatus() != CompletionStatus.FULL_SUCCESS) {
            return false;
        }
        if (context.getRapport() < 30) {
            return false;
        }
        if (context.getComplexity() == TaskComplexity.TRIVIAL) {
            return false;
        }

        // Higher extraversion = more likely to use humor
        return context.getPersonality().getExtraversion() > 60 &&
               ThreadLocalRandom.current().nextInt(100) < 30;
    }

    private static String generateFollowUp(CompletionContext context) {
        if (context.getStatus() != CompletionStatus.FULL_SUCCESS) {
            return null;
        }

        if (context.getComplexity() == TaskComplexity.TRIVIAL) {
            return "Ready for next task.";
        }

        return switch (context.getSpecialization()) {
            case MINER -> "Shall I continue mining?";
            case BUILDER -> "Ready for next construction phase.";
            case GUARD -> "Perimeter will remain secure.";
            case SCOUT -> "Want to see what I found?";
            case FARMER -> "The crops will need tending again soon.";
            case ARTISAN -> "Ready for next crafting operation.";
        };
    }

    // ============== CONVENIENCE METHODS ==============

    /**
     * Quick report generation with minimal parameters.
     */
    public static CompletionReport quickReport(Specialization specialization,
                                               PersonalityTraits personality,
                                               int rapport,
                                               String taskType) {
        return generateReport(
            CompletionContext.builder()
                .specialization(specialization)
                .personality(personality)
                .rapport(rapport)
                .taskType(taskType)
                .build()
        );
    }

    /**
     * Quick mining report.
     */
    public static CompletionReport miningReport(PersonalityTraits personality,
                                               int rapport,
                                               int oreCount,
                                               String resourceType) {
        return generateReport(
            CompletionContext.builder()
                .specialization(Specialization.MINER)
                .personality(personality)
                .rapport(rapport)
                .taskType("mining")
                .complexity(oreCount > 20 ? 50 : 30)
                .addResult("count", oreCount)
                .addResult("resource", resourceType)
                .build()
        );
    }

    /**
     * Quick construction report.
     */
    public static CompletionReport constructionReport(PersonalityTraits personality,
                                                     int rapport,
                                                     String structureType) {
        return generateReport(
            CompletionContext.builder()
                .specialization(Specialization.BUILDER)
                .personality(personality)
                .rapport(rapport)
                .taskType("construction")
                .complexity(60)
                .addResult("structure", structureType)
                .build()
        );
    }

    /**
     * Quick combat report.
     */
    public static CompletionReport combatReport(PersonalityTraits personality,
                                               int rapport,
                                               int enemiesDefeated) {
        return generateReport(
            CompletionContext.builder()
                .specialization(Specialization.GUARD)
                .personality(personality)
                .rapport(rapport)
                .taskType("combat")
                .complexity(enemiesDefeated > 5 ? 60 : 40)
                .addResult("count", enemiesDefeated)
                .build()
        );
    }
}
