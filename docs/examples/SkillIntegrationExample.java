package com.minewright.action;

import com.minewright.entity.ForemanEntity;
import com.minewright.execution.CodeExecutionEngine;
import com.minewright.skill.*;

import java.util.List;
import java.util.Map;

/**
 * Example showing how to integrate the Skill Library System with ActionExecutor.
 *
 * <p>This example demonstrates the recommended integration points for adding
 * Voyager-style skill learning to your MineWright mod.</p>
 */
public class SkillIntegrationExample {

    /**
     * ENHANCED ActionExecutor with Skill Integration
     *
     * Add these fields and methods to your existing ActionExecutor class:
     */
    public static class EnhancedActionExecutor {
        private final ForemanEntity foreman;
        private SkillIntegration skillIntegration;

        public EnhancedActionExecutor(ForemanEntity foreman) {
            this.foreman = foreman;
            // Initialize skill integration
            this.skillIntegration = new SkillIntegration(foreman, this);
        }

        /**
         * ENHANCED: processNaturalLanguageCommand with skill lookup
         *
         * Add this method to check skills before LLM planning:
         */
        public void processNaturalLanguageCommand(String command) {
            // STEP 1: Check skill library FIRST (saves 30-60 seconds!)
            List<Task> skillTasks = skillIntegration.planWithSkills(command);

            if (skillTasks != null && !skillTasks.isEmpty()) {
                // Use skill-generated tasks - no LLM call needed!
                taskQueue.clear();
                taskQueue.addAll(skillTasks);

                currentGoal = "Executing skill: " + skillTasks.get(0).getAction();
                foreman.getMemory().setCurrentGoal(currentGoal);

                sendToGUI(foreman.getEntityName(), "Using learned skill for: " + command);

                MineWrightMod.LOGGER.info("Using skill-based planning for '{}' ({} tasks)",
                    command, skillTasks.size());
                return;
            }

            // STEP 2: Fall back to normal LLM planning if no skill found
            // ... existing LLM planning code ...
            processNaturalLanguageCommandWithLLM(command);
        }

        /**
         * ENHANCED: Record task execution for learning
         *
         * Add this call in your executeTask method after action completes:
         */
        private void executeTask(Task task) {
            // ... existing action creation and execution ...

            BaseAction action = createAction(task);
            action.start();

            // Wait for completion (in your tick loop)
            if (action.isComplete()) {
                ActionResult result = action.getResult();

                // RECORD: Tell skill system about this execution
                skillIntegration.recordExecution(task, result.isSuccess());
            }
        }

        /**
         * OPTIONAL: Manual skill execution
         *
         * Execute a skill directly with context:
         */
        public void executeSkillDirectly(String skillName, Map<String, Object> context) {
            CodeExecutionEngine.ExecutionResult result =
                skillIntegration.executeSkill(skillName, context);

            if (result.isSuccess()) {
                sendToGUI(foreman.getEntityName(), "Skill completed: " + result.getOutput());
            } else {
                sendToGUI(foreman.getEntityName(), "Skill failed: " + result.getError());
            }
        }

        /**
         * OPTIONAL: Get skill statistics
         */
        public void showSkillStatistics() {
            Map<String, Object> stats = skillIntegration.getStatistics();

            sendToGUI(foreman.getEntityName(),
                String.format("Skills: %d, Generated: %d, Executions: %d",
                    stats.get("total"),
                    stats.get("generatedSkills"),
                    stats.get("totalExecutions")));
        }

        // Existing fields and methods...
        private final java.util.concurrent.BlockingQueue<Task> taskQueue =
            new java.util.concurrent.LinkedBlockingQueue<>();
        private String currentGoal;

        private void sendToGUI(String name, String message) {
            // Your existing GUI code
        }

        private void processNaturalLanguageCommandWithLLM(String command) {
            // Your existing LLM planning code
        }

        private BaseAction createAction(Task task) {
            // Your existing action creation code
            return null;
        }
    }

    /**
     * USAGE EXAMPLES
     */
    public static class UsageExamples {

        /**
         * Example 1: Create a custom skill
         */
        public void createCustomSkill() {
            // Create a custom mining skill
            ExecutableSkill customSkill = ExecutableSkill.builder("efficientMining")
                .description("Mine efficiently in a spiral pattern")
                .category("mining")
                .codeTemplate("""
                    var radius = {{radius}};
                    var depth = {{depth}};

                    // Spiral mining pattern
                    for (var layer = 0; layer < depth; layer++) {
                        for (var angle = 0; angle < 360; angle += 45) {
                            var x = Math.floor(startX + radius * Math.cos(angle));
                            var z = Math.floor(startZ + radius * Math.sin(angle));
                            var y = startY - layer;

                            steve.mineBlock(x, y, z);

                            // Place torch every 10 blocks
                            if ((layer * 8 + angle / 45) % 10 === 0) {
                                steve.placeBlock('torch', x, y + 1, z);
                            }
                        }
                    }
                    """)
                .requiredActions("mine", "place")
                .requiredItems("pickaxe", "torch")
                .estimatedTicks(600)
                .applicabilityPattern("spiral.*mine|efficient.*mining")
                .build();

            // Register the skill
            SkillLibrary.getInstance().addSkill(customSkill);
        }

        /**
         * Example 2: Search for applicable skills
         */
        public void searchSkills(String command) {
            // Find skills relevant to a command
            List<Skill> skills = SkillLibrary.getInstance()
                .semanticSearch("build a small wooden shelter");

            // Skills are sorted by relevance and success rate
            for (Skill skill : skills) {
                System.out.println(String.format(
                    "%s: %.1f%% success rate - %s",
                    skill.getName(),
                    skill.getSuccessRate() * 100,
                    skill.getDescription()
                ));
            }
        }

        /**
         * Example 3: Execute a skill with context
         */
        public void executeSkillExample(ForemanEntity foreman) {
            SkillIntegration integration = new SkillIntegration(foreman, null);

            // Prepare execution context
            Map<String, Object> context = Map.of(
                "x", foreman.getBlockX(),
                "y", foreman.getBlockY(),
                "z", foreman.getBlockZ(),
                "depth", 15,
                "direction", "north"
            );

            // Execute the skill
            CodeExecutionEngine.ExecutionResult result =
                integration.executeSkill("digStaircase", context);

            if (result.isSuccess()) {
                System.out.println("Skill executed successfully!");
            } else {
                System.out.println("Skill failed: " + result.getError());
            }
        }

        /**
         * Example 4: Analyze task sequence for learning
         */
        public void learnFromSequence(List<Task> completedTasks) {
            SkillLibrary library = SkillLibrary.getInstance();
            SkillGenerator generator = new SkillGenerator(library);

            // Analyze successful sequence
            List<Skill> newSkills = generator.analyzeTaskSequence(completedTasks, true);

            System.out.println("Generated " + newSkills.size() + " new skills from experience");

            // Each generated skill is automatically added to the library
            for (Skill skill : newSkills) {
                System.out.println("- " + skill.getName() + ": " + skill.getDescription());
            }
        }

        /**
         * Example 5: Configure skill integration
         */
        public void configureIntegration(ForemanEntity foreman) {
            SkillIntegration integration = new SkillIntegration(foreman, null);

            // Configure behavior
            SkillIntegration.SkillIntegrationConfig config = integration.getConfig();

            // Enable/disable features
            config.setLearningEnabled(true);           // Auto-generate skills
            config.setSkillLookupEnabled(true);        // Check skills before LLM
            config.setMinSuccessRateThreshold(0.75);   // Only use 75%+ skills
            config.setMinTasksForLearning(5);          // Need 5+ tasks to learn
            config.setSequenceTimeoutMs(30000);        // 30s timeout for sequences
        }

        /**
         * Example 6: Get skill library statistics
         */
        public void showStatistics() {
            SkillLibrary library = SkillLibrary.getInstance();
            SkillIntegration integration = new SkillIntegration(null, null);

            // Library stats
            Map<String, Integer> libraryStats = library.getStatistics();
            System.out.println("Total skills: " + libraryStats.get("total"));
            System.out.println("Mining skills: " + libraryStats.get("mining"));
            System.out.println("Building skills: " + libraryStats.get("building"));
            System.out.println("Total executions: " + libraryStats.get("totalExecutions"));

            // Integration stats
            Map<String, Object> integrationStats = integration.getStatistics();
            System.out.println("Generated skills: " + integrationStats.get("generatedSkills"));
            System.out.println("Current sequence: " + integrationStats.get("currentSequenceSize"));
        }

        /**
         * Example 7: Find best skill for a task
         */
        public void findBestSkillForTask(Task task) {
            List<Skill> applicableSkills = SkillLibrary.getInstance()
                .findApplicableSkills(task);

            if (applicableSkills.isEmpty()) {
                System.out.println("No applicable skills found");
            } else {
                Skill best = applicableSkills.get(0);
                System.out.println(String.format(
                    "Best skill: %s (%.1f%% success rate)",
                    best.getName(),
                    best.getSuccessRate() * 100
                ));
            }
        }

        /**
         * Example 8: List skills by category
         */
        public void listSkillsByCategory(String category) {
            List<Skill> skills = SkillLibrary.getInstance()
                .getSkillsByCategory(category);

            System.out.println("Skills in category '" + category + "':");
            for (Skill skill : skills) {
                System.out.println(String.format(
                    "  %s: %s (%.1f%% success, %d executions)",
                    skill.getName(),
                    skill.getDescription(),
                    skill.getSuccessRate() * 100,
                    skill.getExecutionCount()
                ));
            }
        }
    }

    /**
     * BUILT-IN SKILLS QUICK REFERENCE
     */
    public static class BuiltInSkillsReference {
        /**
         * Mining Skills:
         * - digStaircase: Create staircases with torch placement
         * - stripMine: Strip mining at Y=-58
         * - branchMine: Branch mining tunnels
         *
         * Building Skills:
         * - buildShelter: Basic 5x5x3 shelter
         * - buildPlatform: Flat building platform
         *
         * Farming Skills:
         * - farmWheat: Automated wheat farming
         * - farmTree: Tree sapling grid planting
         *
         * Utility Skills:
         * - organizeInventory: Sort items by type
         * - collectDrops: Spiral item collection
         */
    }
}
