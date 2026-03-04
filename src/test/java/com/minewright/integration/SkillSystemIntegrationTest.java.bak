package com.minewright.integration;

import com.minewright.skill.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the skill system including skill library,
 * refinement loop, and critic agent.
 *
 * <p><b>Test Coverage:</b></p>
 * <ul>
 *   <li>Skill storage and retrieval</li>
 *   <li>Skill validation and refinement</li>
 *   <li>Critic agent feedback generation</li>
 *   <li>Skill composition and decomposition</li>
 *   <li>Concurrent skill operations</li>
 *   <li>Skill effectiveness tracking</li>
 *   <li>Semantic skill search</li>
 * </ul>
 *
 * @see SkillLibrary
 * @see SkillRefinementLoop
 * @see CriticAgent
 * @see Skill
 * @since 1.0.0
 */
@DisplayName("Skill System Integration Tests")
class SkillSystemIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("Skills can be added and retrieved from library")
    void testSkillStorage() {
        SkillLibrary library = SkillLibrary.getInstance();

        // Create a skill
        Skill skill = Skill.builder()
            .name("mineIronOre")
            .description("Mine iron ore from nearby deposits")
            .code("function mineIronOre() { return findAndMine('iron_ore'); }")
            .category("mining")
            .addTag("mining")
            .addTag("iron")
            .addTag("ore")
            .build();

        // Add to library
        assertTrue(library.addSkill(skill), "Skill should be added");

        // Retrieve by name
        Optional<Skill> retrieved = library.getSkill("mineIronOre");
        assertTrue(retrieved.isPresent(), "Skill should be retrievable");
        assertEquals("mineIronOre", retrieved.get().getName(), "Name should match");
        assertEquals("mining", retrieved.get().getCategory(), "Category should match");
    }

    @Test
    @DisplayName("Skills can be searched semantically")
    void testSemanticSkillSearch() {
        SkillLibrary library = SkillLibrary.getInstance();

        // Add various skills
        library.addSkill(Skill.builder()
            .name("mineIronOre")
            .description("Extract iron ore from underground")
            .code("/* code */")
            .category("mining")
            .build());

        library.addSkill(Skill.builder()
            .name("craftIronPickaxe")
            .description("Craft an iron pickaxe at crafting table")
            .code("/* code */")
            .category("crafting")
            .build());

        library.addSkill(Skill.builder()
            .name("buildShelter")
            .description("Construct a basic shelter for protection")
            .code("/* code */")
            .category("building")
            .build());

        // Search for mining-related skills
        List<Skill> miningSkills = library.searchSkills("mining underground extract");
        assertTrue(miningSkills.size() >= 1, "Should find mining skills");

        // Search for crafting skills
        List<Skill> craftingSkills = library.searchSkills("craft create items");
        assertTrue(craftingSkills.size() >= 1, "Should find crafting skills");
    }

    @Test
    @DisplayName("Critic agent validates skill execution")
    void testCriticAgentValidation() {
        CriticAgent critic = CriticAgent.getInstance();
        critic.start();

        // Create a skill
        Skill skill = Skill.builder()
            .name("testSkill")
            .description("Test skill")
            .code("function test() { return true; }")
            .build();

        // Create successful validation context
        ValidationContext successContext = new ValidationContext(
            "testTask",
            Map.of("result", "success"),
            List.of(),
            true,
            100
        );

        ValidationResult result = critic.validate(skill, successContext);

        assertTrue(result.isValid(), "Successful execution should validate");
        assertNull(result.getReason(), "Success should have no error reason");
        assertTrue(result.getValidationTime() > 0, "Should track validation time");

        // Create failed validation context
        ValidationContext failureContext = new ValidationContext(
            "testTask",
            Map.of(),
            List.of("Error: task failed"),
            false,
            50
        );

        ValidationResult failureResult = critic.validate(skill, failureContext);

        assertFalse(failureResult.isValid(), "Failed execution should not validate");
        assertNotNull(failureResult.getReason(), "Failure should have error reason");

        critic.stop();
    }

    @Test
    @DisplayName("Skill refinement loop iterates to improve skills")
    void testSkillRefinementLoop() throws Exception {
        SkillLibrary library = SkillLibrary.getInstance();
        SkillRefinementLoop refinementLoop = new SkillRefinementLoop(library, 3);
        refinementLoop.start();

        // Create a skill that needs refinement
        Skill initialSkill = Skill.builder()
            .name("inefficientMiner")
            .description("Mine stone slowly")
            .code("function mine() { /* inefficient code */ }")
            .build();

        // Create a context that will trigger refinement
        ValidationContext context = new ValidationContext(
            "mining",
            Map.of("blocks_mined", 5),
            List.of("Too slow"),
            false,
            5000
        );

        // Run refinement
        CompletableFuture<SkillRefinementLoop.RefinementResult> future =
            refinementLoop.refineSkill(initialSkill, context);

        SkillRefinementLoop.RefinementResult result = future.get(30, TimeUnit.SECONDS);

        assertNotNull(result, "Should have refinement result");
        assertTrue(result.getIterations() > 0, "Should have attempted refinements");

        refinementLoop.stop();
    }

    @Test
    @DisplayName("Skill composition creates complex skills")
    void testSkillComposition() {
        SkillLibrary library = SkillLibrary.getInstance();

        // Add primitive skills
        Skill findOre = Skill.builder()
            .name("findOre")
            .description("Find ore deposits")
            .code("function findOre() { /* find ore */ }")
            .category("mining")
            .build();

        Skill mineBlock = Skill.builder()
            .name("mineBlock")
            .description("Mine a single block")
            .code("function mineBlock() { /* mine block */ }")
            .category("mining")
            .build();

        library.addSkill(findOre);
        library.addSkill(mineBlock);

        // Create composed skill
        SkillComposer composer = new SkillComposer(library);

        ComposedSkill composedSkill = composer.compose("mineOreDeposit", "Find and mine ore deposit")
            .addStep("findOre", Map.of())
            .addStep("mineBlock", Map.of("count", 10))
            .build();

        assertNotNull(composedSkill, "Should create composed skill");
        assertEquals(2, composedSkill.getSteps().size(), "Should have 2 steps");
        assertEquals("findOre", composedSkill.getSteps().get(0).getSkillName(),
            "First step should be findOre");
    }

    @Test
    @DisplayName("Skill effectiveness is tracked over time")
    void testSkillEffectivenessTracking() {
        SkillEffectivenessTracker tracker = new SkillEffectivenessTracker();

        String skillName = "testSkill";
        UUID agentId = UUID.randomUUID();

        // Record successful executions
        tracker.recordExecution(skillName, agentId, true, 1000);
        tracker.recordExecution(skillName, agentId, true, 1200);
        tracker.recordExecution(skillName, agentId, true, 900);

        // Record failed execution
        tracker.recordExecution(skillName, agentId, false, 500);

        // Get effectiveness
        double effectiveness = tracker.getEffectiveness(skillName);
        assertTrue(effectiveness > 0.5, "Should have high effectiveness");

        // Get statistics
        SkillEffectivenessTracker.SkillStats stats = tracker.getStatistics(skillName);
        assertNotNull(stats, "Should have statistics");
        assertEquals(4, stats.getTotalExecutions(), "Should track total executions");
        assertEquals(3, stats.getSuccessfulExecutions(), "Should track successful executions");
    }

    @Test
    @DisplayName("Concurrent skill operations are thread-safe")
    void testConcurrentSkillOperations() throws InterruptedException {
        SkillLibrary library = SkillLibrary.getInstance();
        SkillEffectivenessTracker tracker = new SkillEffectivenessTracker();

        int numThreads = 10;
        int operationsPerThread = 50;

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        // Add skills
                        Skill skill = Skill.builder()
                            .name("skill_" + threadId + "_" + j)
                            .description("Test skill")
                            .code("function test() {}")
                            .category("test")
                            .build();

                        if (library.addSkill(skill)) {
                            successCount.incrementAndGet();
                        }

                        // Record executions
                        tracker.recordExecution(skill.getName(), UUID.randomUUID(),
                            j % 4 != 0, 1000 + j);

                        // Search skills
                        library.searchSkills("test");
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(successCount.get() > 0, "Should have successful operations");
    }

    @Test
    @DisplayName("Skills can be categorized and filtered")
    void testSkillCategorization() {
        SkillLibrary library = SkillLibrary.getInstance();

        // Add skills with different categories
        library.addSkill(Skill.builder()
            .name("mineStone")
            .category("mining")
            .addTag("stone")
            .build());

        library.addSkill(Skill.builder()
            .name("mineIron")
            .category("mining")
            .addTag("iron")
            .build());

        library.addSkill(Skill.builder()
            .name("craftPickaxe")
            .category("crafting")
            .addTag("tools")
            .build());

        library.addSkill(Skill.builder()
            .name("buildHouse")
            .category("building")
            .addTag("shelter")
            .build());

        // Get by category
        Collection<Skill> miningSkills = library.getByCategory("mining");
        assertTrue(miningSkills.size() >= 2, "Should have multiple mining skills");

        // Get by tag
        Collection<Skill> toolSkills = library.getByTag("tools");
        assertTrue(toolSkills.size() >= 1, "Should have tool skills");
    }

    @Test
    @DisplayName("Refinement loop statistics are tracked")
    void testRefinementLoopStatistics() {
        SkillLibrary library = SkillLibrary.getInstance();
        SkillRefinementLoop refinementLoop = new SkillRefinementLoop(library, 3);
        refinementLoop.start();

        // Perform some refinements
        Skill skill = Skill.builder()
            .name("testSkill")
            .description("Test")
            .code("function test() {}")
            .build();

        ValidationContext context = new ValidationContext(
            "test",
            Map.of(),
            List.of(),
            true,
            100
        );

        refinementLoop.refineSkill(skill, context);

        Map<String, Object> stats = refinementLoop.getStatistics();
        assertTrue((Integer) stats.get("totalRefinements") > 0,
            "Should track refinements");
        assertTrue((Integer) stats.get("maxIterations") == 3,
            "Should track max iterations");

        refinementLoop.stop();
    }

    @Test
    @DisplayName("Skills can be exported and imported")
    void testSkillExportImport() {
        SkillLibrary library = SkillLibrary.getInstance();

        // Add skills
        library.addSkill(Skill.builder()
            .name("skill1")
            .description("First skill")
            .code("code1")
            .category("test")
            .build());

        library.addSkill(Skill.builder()
            .name("skill2")
            .description("Second skill")
            .code("code2")
            .category("test")
            .build());

        // Export
        String exported = library.exportSkills();
        assertNotNull(exported, "Export should produce string");
        assertTrue(exported.length() > 0, "Export should have content");

        // Clear and import
        library.clear();
        assertEquals(0, library.getAllSkills().size(), "Should be empty after clear");

        library.importSkills(exported);
        assertTrue(library.getAllSkills().size() >= 2, "Should import skills");
    }

    @Test
    @DisplayName("Critic agent provides statistics")
    void testCriticAgentStatistics() {
        CriticAgent critic = CriticAgent.getInstance();
        critic.start();

        Skill skill = Skill.builder()
            .name("testSkill")
            .description("Test")
            .code("function test() {}")
            .build();

        // Perform validations
        for (int i = 0; i < 10; i++) {
            ValidationContext context = new ValidationContext(
                "test",
                Map.of(),
                List.of(),
                i % 3 == 0, // Mix of success/failure
                100 + i * 10
            );
            critic.validate(skill, context);
        }

        Map<String, Object> stats = critic.getStatistics();
        assertTrue((Integer) stats.get("validationsPerformed") >= 10,
            "Should track validations");
        assertTrue((Double) stats.get("successRate") >= 0.0 &&
                   (Double) stats.get("successRate") <= 1.0,
            "Success rate should be valid");

        critic.stop();
    }

    @Test
    @DisplayName("Skill dependency validation")
    void testSkillDependencyValidation() {
        SkillLibrary library = SkillLibrary.getInstance();

        // Add base skills
        library.addSkill(Skill.builder()
            .name("findBlock")
            .description("Find a block")
            .code("function findBlock() {}")
            .category("utility")
            .build());

        library.addSkill(Skill.builder()
            .name("breakBlock")
            .description("Break a block")
            .code("function breakBlock() {}")
            .category("utility")
            .build());

        // Create composed skill with dependencies
        SkillComposer composer = new SkillComposer(library);

        ComposedSkill composedSkill = composer.compose("mineBlock", "Mine a block")
            .addStep("findBlock", Map.of())
            .addStep("breakBlock", Map.of())
            .build();

        // Validate dependencies
        ValidationResult validation = composedSkill.validateDependencies(library);

        assertTrue(validation.isValid(), "Dependencies should be valid");
    }

    @Test
    @DisplayName("Skill learning loop extracts patterns")
    void testSkillLearningLoop() {
        SkillLibrary library = SkillLibrary.getInstance();
        SkillLearningLoop learningLoop = new SkillLearningLoop(library);
        learningLoop.start();

        // Create execution sequences
        ExecutionSequence sequence1 = new ExecutionSequence();
        sequence1.addAction("findBlock", Map.of("type", "iron_ore"));
        sequence1.addAction("moveTo", Map.of("target", "block"));
        sequence1.addAction("breakBlock", Map.of());
        sequence1.markSuccessful();

        ExecutionSequence sequence2 = new ExecutionSequence();
        sequence2.addAction("findBlock", Map.of("type", "coal_ore"));
        sequence2.addAction("moveTo", Map.of("target", "block"));
        sequence2.addAction("breakBlock", Map.of());
        sequence2.markSuccessful();

        // Extract patterns
        List<TaskPattern> patterns = learningLoop.extractPatterns(
            Arrays.asList(sequence1, sequence2));

        assertTrue(patterns.size() > 0, "Should extract patterns");

        learningLoop.stop();
    }

    @Test
    @DisplayName("Skill auto-generation from sequences")
    void testSkillAutoGeneration() {
        SkillLibrary library = SkillLibrary.getInstance();
        SkillAutoGenerator generator = new SkillAutoGenerator(library);

        // Create execution sequence
        ExecutionSequence sequence = new ExecutionSequence();
        sequence.addAction("findNearest", Map.of("type", "tree"));
        sequence.addAction("moveTo", Map.of("target", "tree"));
        sequence.addAction("breakBlock", Map.of());
        sequence.addAction("collectItem", Map.of());
        sequence.markSuccessful();

        // Generate skill
        Optional<Skill> generatedSkill = generator.generateSkill(
            "gatherWood",
            "Gather wood from nearby trees",
            sequence
        );

        assertTrue(generatedSkill.isPresent(), "Should generate skill");

        Skill skill = generatedSkill.get();
        assertEquals("gatherWood", skill.getName(), "Name should match");
        assertTrue(skill.getCode().length() > 0, "Should have code");
    }

    @Test
    @DisplayName("Multiple refinement loops can run concurrently")
    void testConcurrentRefinement() throws InterruptedException {
        SkillLibrary library = SkillLibrary.getInstance();
        SkillRefinementLoop refinementLoop = new SkillRefinementLoop(library, 3);
        refinementLoop.start();

        int numRefinements = 10;
        ExecutorService executor = Executors.newFixedThreadPool(5);
        CountDownLatch latch = new CountDownLatch(numRefinements);
        AtomicInteger completedCount = new AtomicInteger(0);

        for (int i = 0; i < numRefinements; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    Skill skill = Skill.builder()
                        .name("skill_" + index)
                        .description("Test skill " + index)
                        .code("function test() {}")
                        .build();

                    ValidationContext context = new ValidationContext(
                        "test",
                        Map.of(),
                        List.of(),
                        true,
                        100
                    );

                    CompletableFuture<SkillRefinementLoop.RefinementResult> future =
                        refinementLoop.refineSkill(skill, context);

                    future.get(30, TimeUnit.SECONDS);
                    completedCount.incrementAndGet();
                } catch (Exception e) {
                    // Expected to have some failures
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(60, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completedCount.get() > 0, "Should complete some refinements");

        refinementLoop.stop();
    }

    @Test
    @DisplayName("Skill execution tracking")
    void testSkillExecutionTracking() {
        ExecutionTracker tracker = new ExecutionTracker();

        String skillName = "testSkill";
        UUID agentId = UUID.randomUUID();

        // Start execution
        String executionId = tracker.startExecution(skillName, agentId);
        assertNotNull(executionId, "Should get execution ID");

        // Check in-progress
        assertTrue(tracker.isInProgress(executionId), "Should be in progress");

        // Complete execution
        tracker.completeExecution(executionId, true, Map.of("result", "success"));

        assertFalse(tracker.isInProgress(executionId), "Should not be in progress");

        // Get history
        List<ExecutionTracker.ExecutionRecord> history = tracker.getExecutionHistory(skillName);
        assertTrue(history.size() > 0, "Should have execution history");
    }
}
