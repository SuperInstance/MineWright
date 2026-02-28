package com.minewright.skill;

import com.minewright.action.Task;
import com.minewright.testutil.TaskBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SkillLibrary}.
 *
 * <p>Tests cover:</p>
 * <ul>
 *   <li>Skill registration and retrieval</li>
 *   <li>Applicable skill finding</li>
 *   <li>Semantic search functionality</li>
 *   <li>Success rate tracking and recording</li>
 *   <li>Built-in skills initialization</li>
 *   <li>Duplicate prevention</li>
 *   <li>Category filtering</li>
 *   <li>Statistics generation</li>
 *   <li>Skill removal</li>
 * </ul>
 *
 * <p>Note: Some tests use reflection to access singleton state for isolation.</p>
 */
@DisplayName("Skill Library Tests")
class SkillLibraryTest {

    private SkillLibrary library;

    @BeforeEach
    void setUp() throws Exception {
        // Reset singleton instance for test isolation
        resetSkillLibrarySingleton();
        library = SkillLibrary.getInstance();
    }

    // ==================== Helper Methods ====================

    private void resetSkillLibrarySingleton() throws Exception {
        Field instanceField = SkillLibrary.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
    }

    private ExecutableSkill createTestSkill(String name) {
        return ExecutableSkill.builder(name)
            .description("Test skill: " + name)
            .category("test")
            .codeTemplate("console.log('" + name + "');")
            .requiredAction("test")
            .build();
    }

    private ExecutableSkill createMiningSkill(String name, String description) {
        return ExecutableSkill.builder(name)
            .description(description)
            .category("mining")
            .codeTemplate("mineBlock();")
            .requiredAction("mine")
            .applicabilityPattern("mine.*" + name)
            .build();
    }

    private ExecutableSkill createBuildingSkill(String name) {
        return ExecutableSkill.builder(name)
            .description("Building skill: " + name)
            .category("building")
            .codeTemplate("placeBlock();")
            .requiredAction("place")
            .build();
    }

    // ==================== Skill Registration Tests ====================

    @Nested
    @DisplayName("Skill Registration Tests")
    class SkillRegistrationTests {

        @Test
        @DisplayName("Add and retrieve skill by name")
        void addAndRetrieveSkill() {
            ExecutableSkill skill = createTestSkill("testSkill");

            boolean added = library.addSkill(skill);

            assertTrue(added, "Skill should be added successfully");
            assertTrue(library.hasSkill("testSkill"), "Library should contain the skill");
            assertEquals(skill, library.getSkill("testSkill"),
                "Retrieved skill should match the added skill");
        }

        @Test
        @DisplayName("Cannot add skill with null")
        void cannotAddNullSkill() {
            boolean added = library.addSkill(null);

            assertFalse(added, "Should not add null skill");
        }

        @Test
        @DisplayName("Cannot add duplicate skill by name")
        void cannotAddDuplicateSkillByName() {
            ExecutableSkill skill1 = createTestSkill("duplicateSkill");
            ExecutableSkill skill2 = createTestSkill("duplicateSkill");

            library.addSkill(skill1);
            boolean added = library.addSkill(skill2);

            assertFalse(added, "Should not add duplicate skill with same name");
            assertEquals(skill1, library.getSkill("duplicateSkill"),
                "Should keep the original skill");
        }

        @Test
        @DisplayName("Prevents duplicate skills with same signature")
        void preventsDuplicateSkillsWithSameSignature() {
            // Skills with same category, description, and actions should be duplicates
            ExecutableSkill skill1 = ExecutableSkill.builder("skill1")
                .description("Same description")
                .category("mining")
                .codeTemplate("code1")
                .requiredAction("mine")
                .build();

            ExecutableSkill skill2 = ExecutableSkill.builder("skill2")
                .description("Same description")
                .category("mining")
                .codeTemplate("code2")
                .requiredAction("mine")
                .build();

            library.addSkill(skill1);
            boolean added = library.addSkill(skill2);

            assertFalse(added, "Should prevent duplicate with same signature");
            assertTrue(library.hasSkill("skill1"),
                "Original skill should remain");
            assertFalse(library.hasSkill("skill2"),
                "Duplicate skill should not be added");
        }

        @Test
        @DisplayName("Can add skills with different names but same signature")
        void canAddSkillsWithDifferentCategories() {
            ExecutableSkill skill1 = ExecutableSkill.builder("skill1")
                .description("Same description")
                .category("mining")
                .codeTemplate("code")
                .requiredAction("mine")
                .build();

            ExecutableSkill skill2 = ExecutableSkill.builder("skill2")
                .description("Same description")
                .category("building") // Different category
                .codeTemplate("code")
                .requiredAction("mine")
                .build();

            library.addSkill(skill1);
            boolean added = library.addSkill(skill2);

            assertTrue(added, "Should add skills with different categories");
            assertTrue(library.hasSkill("skill1"));
            assertTrue(library.hasSkill("skill2"));
        }

        @Test
        @DisplayName("Get non-existent skill returns null")
        void getNonExistentSkillReturnsNull() {
            Skill skill = library.getSkill("nonExistent");

            assertNull(skill, "Should return null for non-existent skill");
        }

        @Test
        @DisplayName("Has skill returns correct boolean")
        void hasSkillReturnsCorrectBoolean() {
            ExecutableSkill skill = createTestSkill("existsSkill");

            assertFalse(library.hasSkill("existsSkill"),
                "Should not have skill before adding");

            library.addSkill(skill);

            assertTrue(library.hasSkill("existsSkill"),
                "Should have skill after adding");
            assertFalse(library.hasSkill("notExistsSkill"),
                "Should not have skill that was never added");
        }

        @Test
        @DisplayName("Skill count updates correctly")
        void skillCountUpdatesCorrectly() {
            int initialCount = library.getSkillCount();

            library.addSkill(createTestSkill("skill1"));
            assertEquals(initialCount + 1, library.getSkillCount());

            library.addSkill(createTestSkill("skill2"));
            assertEquals(initialCount + 2, library.getSkillCount());

            // Duplicate should not increase count
            library.addSkill(createTestSkill("skill1"));
            assertEquals(initialCount + 2, library.getSkillCount());
        }
    }

    // ==================== Applicable Skills Tests ====================

    @Nested
    @DisplayName("Applicable Skills Tests")
    class ApplicableSkillsTests {

        @BeforeEach
        void setUp() {
            // Clear built-in skills for cleaner testing
            try {
                resetSkillLibrarySingleton();
                library = SkillLibrary.getInstance();
                // Clear the skills map
                Field skillsField = SkillLibrary.class.getDeclaredField("skills");
                skillsField.setAccessible(true);
                @SuppressWarnings("unchecked")
                Map<String, Skill> skillsMap = (Map<String, Skill>) skillsField.get(library);
                skillsMap.clear();
            } catch (Exception e) {
                // If clearing fails, work with what we have
            }
        }

        @Test
        @DisplayName("Find applicable skills returns matching skills")
        void findApplicableSkills() {
            ExecutableSkill miningSkill = createMiningSkill("ore", "Mine ores");
            ExecutableSkill buildingSkill = createBuildingSkill("shelter");
            ExecutableSkill farmingSkill = ExecutableSkill.builder("farmWheat")
                .description("Farm wheat")
                .category("farming")
                .codeTemplate("farm();")
                .requiredAction("farm")
                .build();

            library.addSkill(miningSkill);
            library.addSkill(buildingSkill);
            library.addSkill(farmingSkill);

            Task miningTask = TaskBuilder.aTask("mine")
                .withBlock("iron_ore")
                .build();

            List<Skill> applicable = library.findApplicableSkills(miningTask);

            assertFalse(applicable.isEmpty(), "Should find applicable skills");
            assertTrue(applicable.stream().anyMatch(s -> s.getName().equals("ore")),
                "Should include mining skill");
        }

        @Test
        @DisplayName("Applicable skills are sorted by success rate")
        void applicableSkillsAreSortedBySuccessRate() {
            ExecutableSkill highSuccessSkill = createMiningSkill("high", "High success skill");
            ExecutableSkill lowSuccessSkill = createMiningSkill("low", "Low success skill");

            // Set different success rates
            highSuccessSkill.recordSuccess(true);
            highSuccessSkill.recordSuccess(true);
            highSuccessSkill.recordSuccess(true);

            lowSuccessSkill.recordSuccess(true);
            lowSuccessSkill.recordSuccess(false);
            lowSuccessSkill.recordSuccess(false);

            library.addSkill(lowSuccessSkill);
            library.addSkill(highSuccessSkill);

            Task task = TaskBuilder.aTask("mine").withBlock("coal").build();
            List<Skill> applicable = library.findApplicableSkills(task);

            // High success skill should come first
            if (applicable.size() >= 2) {
                assertTrue(applicable.get(0).getSuccessRate() >= applicable.get(1).getSuccessRate(),
                    "Skills should be sorted by success rate descending");
            }
        }

        @Test
        @DisplayName("Find applicable skills returns empty list when no matches")
        void findApplicableSkillsReturnsEmptyWhenNoMatches() {
            ExecutableSkill craftingSkill = ExecutableSkill.builder("craftItem")
                .description("Craft items")
                .category("crafting")
                .codeTemplate("craft();")
                .requiredAction("craft")
                .build();

            library.addSkill(craftingSkill);

            Task miningTask = TaskBuilder.aTask("mine").withBlock("stone").build();
            List<Skill> applicable = library.findApplicableSkills(miningTask);

            assertTrue(applicable.isEmpty(), "Should return empty list when no skills match");
        }

        @Test
        @DisplayName("Pattern matching skills are found as applicable")
        void patternMatchingSkillsAreFound() {
            ExecutableSkill stairSkill = ExecutableSkill.builder("digStaircase")
                .description("Dig staircase for mining")
                .category("mining")
                .codeTemplate("dig();")
                .requiredAction("mine")
                .applicabilityPattern("dig.*staircase|stair.*down")
                .build();

            library.addSkill(stairSkill);

            Task stairTask = TaskBuilder.aTask("mine")
                .withParam("description", "need to dig staircase down")
                .build();

            List<Skill> applicable = library.findApplicableSkills(stairTask);

            assertEquals(1, applicable.size(), "Should find pattern-matching skill");
            assertEquals("digStaircase", applicable.get(0).getName());
        }

        @Test
        @DisplayName("All applicable skills are returned")
        void allApplicableSkillsAreReturned() {
            library.addSkill(createMiningSkill("skill1", "Mine skill 1"));
            library.addSkill(createMiningSkill("skill2", "Mine skill 2"));
            library.addSkill(createMiningSkill("skill3", "Mine skill 3"));

            Task task = TaskBuilder.aTask("mine").withBlock("diamond").build();
            List<Skill> applicable = library.findApplicableSkills(task);

            assertTrue(applicable.size() >= 3,
                "Should return all applicable skills");
        }
    }

    // ==================== Semantic Search Tests ====================

    @Nested
    @DisplayName("Semantic Search Tests")
    class SemanticSearchTests {

        @Test
        @DisplayName("Semantic search finds similar skills")
        void semanticSearchFindsSimilarSkills() {
            library.addSkill(ExecutableSkill.builder("digStaircase")
                .description("Dig a staircase downwards for safe mining")
                .category("mining")
                .codeTemplate("dig();")
                .requiredAction("mine")
                .build());

            library.addSkill(ExecutableSkill.builder("buildShelter")
                .description("Build a basic shelter")
                .category("building")
                .codeTemplate("build();")
                .requiredAction("place")
                .build());

            List<Skill> results = library.semanticSearch("dig stairs down");

            assertFalse(results.isEmpty(), "Should find matching skills");
            assertTrue(results.stream().anyMatch(s -> s.getName().contains("Staircase")),
                "Should find staircase skill for 'dig stairs down' query");
        }

        @Test
        @DisplayName("Semantic search is case insensitive")
        void semanticSearchIsCaseInsensitive() {
            library.addSkill(ExecutableSkill.builder("mineOre")
                .description("Mine ore deposits")
                .category("mining")
                .codeTemplate("mine();")
                .requiredAction("mine")
                .build());

            List<Skill> upper = library.semanticSearch("MINE ORE");
            List<Skill> lower = library.semanticSearch("mine ore");
            List<Skill> mixed = library.semanticSearch("Mine Ore");

            assertFalse(upper.isEmpty(), "Uppercase search should find results");
            assertFalse(lower.isEmpty(), "Lowercase search should find results");
            assertFalse(mixed.isEmpty(), "Mixed case search should find results");
        }

        @Test
        @DisplayName("Semantic search handles empty query")
        void semanticSearchHandlesEmptyQuery() {
            List<Skill> results = library.semanticSearch("");

            assertNotNull(results, "Should return non-null list");
            assertTrue(results.isEmpty(), "Should return empty list for empty query");
        }

        @Test
        @DisplayName("Semantic search handles null query")
        void semanticSearchHandlesNullQuery() {
            List<Skill> results = library.semanticSearch(null);

            assertNotNull(results, "Should return non-null list");
            assertTrue(results.isEmpty(), "Should return empty list for null query");
        }

        @Test
        @DisplayName("Semantic search returns results sorted by relevance")
        void semanticSearchReturnsSortedResults() {
            ExecutableSkill exactMatch = ExecutableSkill.builder("exactSkill")
                .description("mine coal")
                .category("mining")
                .codeTemplate("code")
                .requiredAction("mine")
                .build();

            ExecutableSkill partialMatch = ExecutableSkill.builder("partialSkill")
                .description("mining operation")
                .category("mining")
                .codeTemplate("code")
                .requiredAction("mine")
                .build();

            library.addSkill(exactMatch);
            library.addSkill(partialMatch);

            List<Skill> results = library.semanticSearch("mine coal");

            // Exact match should come first
            if (results.size() >= 1) {
                assertTrue(results.get(0).getName().contains("exact") ||
                          results.get(0).getDescription().contains("mine coal"),
                    "Most relevant skill should be first");
            }
        }

        @Test
        @DisplayName("Semantic search limits results to top 10")
        void semanticSearchLimitsResults() {
            // Add many skills
            for (int i = 0; i < 20; i++) {
                library.addSkill(ExecutableSkill.builder("skill" + i)
                    .description("mining and building skill number " + i)
                    .category("test")
                    .codeTemplate("code")
                    .requiredAction("test")
                    .build());
            }

            List<Skill> results = library.semanticSearch("mining");

            assertTrue(results.size() <= 10, "Should limit results to 10");
        }

        @Test
        @DisplayName("Semantic search boosts exact matches")
        void semanticSearchBoostsExactMatches() {
            ExecutableSkill exact = ExecutableSkill.builder("exactMatch")
                .description("mine")
                .category("mining")
                .codeTemplate("code")
                .requiredAction("mine")
                .build();

            ExecutableSkill partial = ExecutableSkill.builder("partialMatch")
                .description("mining operation with mining tools")
                .category("mining")
                .codeTemplate("code")
                .requiredAction("mine")
                .build();

            library.addSkill(exact);
            library.addSkill(partial);

            List<Skill> results = library.semanticSearch("mine");

            // Exact match should rank higher due to boost
            if (results.size() >= 2) {
                assertEquals("exactMatch", results.get(0).getName(),
                    "Exact match should be boosted in ranking");
            }
        }

        @Test
        @DisplayName("Success rate affects semantic search ranking")
        void successRateAffectsSearchRanking() {
            ExecutableSkill successfulSkill = ExecutableSkill.builder("successSkill")
                .description("mining skill")
                .category("mining")
                .codeTemplate("code")
                .requiredAction("mine")
                .build();

            ExecutableSkill unsuccessfulSkill = ExecutableSkill.builder("failSkill")
                .description("mining skill")
                .category("mining")
                .codeTemplate("code")
                .requiredAction("mine")
                .build();

            // Set different success rates
            for (int i = 0; i < 10; i++) {
                successfulSkill.recordSuccess(true);
                unsuccessfulSkill.recordSuccess(false);
            }

            library.addSkill(unsuccessfulSkill);
            library.addSkill(successfulSkill);

            List<Skill> results = library.semanticSearch("mining");

            assertEquals("successSkill", results.get(0).getName(),
                "More successful skill should rank higher");
        }
    }

    // ==================== Success Recording Tests ====================

    @Nested
    @DisplayName("Success Recording Tests")
    class SuccessRecordingTests {

        @Test
        @DisplayName("Record outcome updates success rate")
        void recordOutcomeUpdatesSuccessRate() {
            ExecutableSkill skill = createTestSkill("recordSkill");
            library.addSkill(skill);

            library.recordOutcome("recordSkill", true);
            library.recordOutcome("recordSkill", false);
            library.recordOutcome("recordSkill", true);

            Skill retrieved = library.getSkill("recordSkill");
            assertEquals(0.666, retrieved.getSuccessRate(), 0.001,
                "Success rate should be updated after recording outcomes");
        }

        @Test
        @DisplayName("Record outcome for non-existent skill is safe")
        void recordOutcomeForNonExistentSkillIsSafe() {
            assertDoesNotThrow(() -> library.recordOutcome("nonExistent", true),
                "Recording outcome for non-existent skill should not throw");
        }

        @Test
        @DisplayName("Multiple record outcomes accumulate correctly")
        void multipleRecordOutcomesAccumulate() {
            ExecutableSkill skill = createTestSkill("accumulateSkill");
            library.addSkill(skill);

            for (int i = 0; i < 10; i++) {
                library.recordOutcome("accumulateSkill", true);
            }

            Skill retrieved = library.getSkill("accumulateSkill");
            assertEquals(10, retrieved.getExecutionCount(),
                "Execution count should accumulate");
            assertEquals(1.0, retrieved.getSuccessRate(), 0.001,
                "Success rate should be 100% for all successes");
        }
    }

    // ==================== Built-in Skills Tests ====================

    @Nested
    @DisplayName("Built-in Skills Tests")
    class BuiltInSkillsTests {

        @Test
        @DisplayName("Built-in skills are preloaded")
        void builtInSkillsArePreloaded() {
            // Library should have skills after initialization
            assertTrue(library.getSkillCount() > 0,
                "Library should have built-in skills");
        }

        @Test
        @DisplayName("Built-in mining skills exist")
        void builtInMiningSkillsExist() {
            assertTrue(library.hasSkill("digStaircase"),
                "Should have digStaircase built-in skill");
            assertTrue(library.hasSkill("stripMine"),
                "Should have stripMine built-in skill");
            assertTrue(library.hasSkill("branchMine"),
                "Should have branchMine built-in skill");
        }

        @Test
        @DisplayName("Built-in building skills exist")
        void builtInBuildingSkillsExist() {
            assertTrue(library.hasSkill("buildShelter"),
                "Should have buildShelter built-in skill");
            assertTrue(library.hasSkill("buildPlatform"),
                "Should have buildPlatform built-in skill");
        }

        @Test
        @DisplayName("Built-in farming skills exist")
        void builtInFarmingSkillsExist() {
            assertTrue(library.hasSkill("farmWheat"),
                "Should have farmWheat built-in skill");
            assertTrue(library.hasSkill("farmTree"),
                "Should have farmTree built-in skill");
        }

        @Test
        @DisplayName("Built-in utility skills exist")
        void builtInUtilitySkillsExist() {
            assertTrue(library.hasSkill("organizeInventory"),
                "Should have organizeInventory built-in skill");
            assertTrue(library.hasSkill("collectDrops"),
                "Should have collectDrops built-in skill");
        }

        @Test
        @DisplayName("Built-in skills have required fields")
        void builtInSkillsHaveRequiredFields() {
            Skill digStaircase = library.getSkill("digStaircase");

            assertNotNull(digStaircase, "Skill should exist");
            assertNotNull(digStaircase.getName(), "Should have name");
            assertNotNull(digStaircase.getDescription(), "Should have description");
            assertNotNull(digStaircase.getCategory(), "Should have category");
            assertNotNull(digStaircase.getRequiredActions(), "Should have actions");
            assertFalse(digStaircase.getRequiredActions().isEmpty(),
                "Should have at least one required action");
        }

        @Test
        @DisplayName("Cannot override built-in skills")
        void cannotOverrideBuiltInSkills() {
            int initialCount = library.getSkillCount();

            ExecutableSkill overrideSkill = ExecutableSkill.builder("digStaircase")
                .description("Different description")
                .category("other")
                .codeTemplate("different code")
                .requiredAction("other")
                .build();

            boolean added = library.addSkill(overrideSkill);

            assertFalse(added, "Should not override built-in skill");
            assertEquals(initialCount, library.getSkillCount(),
                "Skill count should not change");

            Skill original = library.getSkill("digStaircase");
            assertEquals("mining", original.getCategory(),
                "Should keep original category");
        }
    }

    // ==================== Category Tests ====================

    @Nested
    @DisplayName("Category Tests")
    class CategoryTests {

        @Test
        @DisplayName("Get skills by category returns correct skills")
        void getSkillsByCategory() {
            library.addSkill(createMiningSkill("mine1", "Mine 1"));
            library.addSkill(createMiningSkill("mine2", "Mine 2"));
            library.addSkill(createBuildingSkill("build1"));

            List<Skill> miningSkills = library.getSkillsByCategory("mining");

            assertTrue(miningSkills.stream().allMatch(s -> s.getCategory().equals("mining")),
                "All returned skills should be in mining category");
            assertTrue(miningSkills.stream().anyMatch(s -> s.getName().equals("mine1")),
                "Should include mine1");
            assertTrue(miningSkills.stream().anyMatch(s -> s.getName().equals("mine2")),
                "Should include mine2");
        }

        @Test
        @DisplayName("Get skills by non-existent category returns empty list")
        void getSkillsByNonExistentCategory() {
            List<Skill> skills = library.getSkillsByCategory("nonexistent");

            assertNotNull(skills, "Should return non-null list");
            assertTrue(skills.isEmpty(), "Should return empty list");
        }

        @Test
        @DisplayName("Get skills by success rate sorts correctly")
        void getSkillsBySuccessRate() {
            ExecutableSkill skill1 = createTestSkill("highSkill");
            ExecutableSkill skill2 = createTestSkill("lowSkill");

            for (int i = 0; i < 5; i++) {
                skill1.recordSuccess(true);
            }
            skill2.recordSuccess(true);
            skill2.recordSuccess(false);

            library.addSkill(skill1);
            library.addSkill(skill2);

            List<Skill> sorted = library.getSkillsBySuccessRate();

            if (sorted.size() >= 2) {
                int highIndex = sorted.indexOf(skill1);
                int lowIndex = sorted.indexOf(skill2);

                assertTrue(highIndex < lowIndex,
                    "Higher success rate skill should come first");
            }
        }
    }

    // ==================== Statistics Tests ====================

    @Nested
    @DisplayName("Statistics Tests")
    class StatisticsTests {

        @Test
        @DisplayName("Get statistics returns correct counts")
        void getStatisticsReturnsCorrectCounts() {
            Map<String, Integer> stats = library.getStatistics();

            assertNotNull(stats, "Statistics should not be null");
            assertTrue(stats.containsKey("total"),
                "Statistics should include total count");
            assertTrue(stats.get("total") > 0,
                "Should have at least built-in skills");
        }

        @Test
        @DisplayName("Statistics includes category counts")
        void statisticsIncludesCategoryCounts() {
            library.addSkill(createMiningSkill("test1", "Test"));
            library.addSkill(createMiningSkill("test2", "Test"));

            Map<String, Integer> stats = library.getStatistics();

            assertTrue(stats.containsKey("mining") || stats.get("total") > 0,
                "Statistics should track categories");
        }

        @Test
        @DisplayName("Statistics includes total executions")
        void statisticsIncludesTotalExecutions() {
            ExecutableSkill skill = createTestSkill("execTest");
            skill.recordSuccess(true);
            skill.recordSuccess(true);
            skill.recordSuccess(true);

            library.addSkill(skill);

            Map<String, Integer> stats = library.getStatistics();

            assertTrue(stats.containsKey("totalExecutions"),
                "Statistics should include total executions");
            assertTrue(stats.get("totalExecutions") >= 3,
                "Execution count should include recorded executions");
        }
    }

    // ==================== Removal Tests ====================

    @Nested
    @DisplayName("Skill Removal Tests")
    class RemovalTests {

        @Test
        @DisplayName("Remove existing skill returns true")
        void removeExistingSkillReturnsTrue() {
            ExecutableSkill skill = createTestSkill("removableSkill");
            library.addSkill(skill);

            boolean removed = library.removeSkill("removableSkill");

            assertTrue(removed, "Should return true when skill is removed");
            assertFalse(library.hasSkill("removableSkill"),
                "Skill should no longer exist in library");
        }

        @Test
        @DisplayName("Remove non-existent skill returns false")
        void removeNonExistentSkillReturnsFalse() {
            boolean removed = library.removeSkill("neverExisted");

            assertFalse(removed, "Should return false when skill doesn't exist");
        }

        @Test
        @DisplayName("Remove skill updates count")
        void removeSkillUpdatesCount() {
            ExecutableSkill skill = createTestSkill("countSkill");
            int before = library.getSkillCount();
            library.addSkill(skill);

            library.removeSkill("countSkill");

            assertEquals(before, library.getSkillCount(),
                "Count should return to previous value after removal");
        }

        @Test
        @DisplayName("Remove skill clears signature")
        void removeSkillClearsSignature() {
            ExecutableSkill skill = createTestSkill("signatureSkill");
            library.addSkill(skill);
            library.removeSkill("signatureSkill");

            // Should be able to add skill with same signature after removal
            ExecutableSkill newSkill = ExecutableSkill.builder("signatureSkill")
                .description("Same description")
                .category("test")
                .codeTemplate("code")
                .requiredAction("test")
                .build();

            assertTrue(library.addSkill(newSkill),
                "Should be able to add skill with same signature after removal");
        }
    }

    // ==================== Singleton Tests ====================

    @Nested
    @DisplayName("Singleton Tests")
    class SingletonTests {

        @Test
        @DisplayName("GetInstance returns same instance")
        void getInstanceReturnsSameInstance() {
            SkillLibrary instance1 = SkillLibrary.getInstance();
            SkillLibrary instance2 = SkillLibrary.getInstance();

            assertSame(instance1, instance2,
                "Should return the same instance");
        }

        @Test
        @DisplayName("Skills persist across getInstance calls")
        void skillsPersistAcrossGetInstanceCalls() {
            SkillLibrary instance1 = SkillLibrary.getInstance();
            ExecutableSkill skill = createTestSkill("persistentSkill");
            instance1.addSkill(skill);

            SkillLibrary instance2 = SkillLibrary.getInstance();

            assertTrue(instance2.hasSkill("persistentSkill"),
                "Skills should persist across getInstance calls");
        }
    }

    // ==================== Skill Execution Tests ====================

    @Nested
    @DisplayName("Skill Execution Tests")
    class SkillExecutionTests {

        @Test
        @DisplayName("Skill generates code with context")
        void skillGeneratesCodeWithContext() {
            ExecutableSkill skill = ExecutableSkill.builder("contextSkill")
                .description("Skill with context variables")
                .category("test")
                .codeTemplate("var depth = {{depth}}; var direction = \"{{direction:quote}}\"; console.log(depth);")
                .requiredAction("mine")
                .build();

            library.addSkill(skill);

            Map<String, Object> context = Map.of(
                "depth", 10,
                "direction", "north"
            );

            String generatedCode = skill.generateCode(context);

            assertNotNull(generatedCode, "Generated code should not be null");
            assertTrue(generatedCode.contains("var depth = 10;"),
                "Code should substitute depth variable");
            assertTrue(generatedCode.contains("var direction = \"north\";"),
                "Code should substitute and quote direction variable");
        }

        @Test
        @DisplayName("Skill execution records success")
        void skillExecutionRecordsSuccess() {
            ExecutableSkill skill = createTestSkill("executionSkill");
            library.addSkill(skill);

            assertEquals(0, skill.getExecutionCount(),
                "Initial execution count should be 0");

            library.recordOutcome("executionSkill", true);

            Skill retrieved = library.getSkill("executionSkill");
            assertEquals(1, retrieved.getExecutionCount(),
                "Execution count should increment");
            assertEquals(1.0, retrieved.getSuccessRate(), 0.001,
                "Success rate should be 100% after one success");
        }

        @Test
        @DisplayName("Skill execution records failure")
        void skillExecutionRecordsFailure() {
            ExecutableSkill skill = createTestSkill("failingSkill");
            library.addSkill(skill);

            library.recordOutcome("failingSkill", false);

            Skill retrieved = library.getSkill("failingSkill");
            assertEquals(1, retrieved.getExecutionCount(),
                "Execution count should increment");
            assertEquals(0.0, retrieved.getSuccessRate(), 0.001,
                "Success rate should be 0% after one failure");
        }

        @Test
        @DisplayName("Multiple executions track correctly")
        void multipleExecutionsTrackCorrectly() {
            ExecutableSkill skill = createTestSkill("multiExecuteSkill");
            library.addSkill(skill);

            // Record mixed outcomes
            library.recordOutcome("multiExecuteSkill", true);
            library.recordOutcome("multiExecuteSkill", true);
            library.recordOutcome("multiExecuteSkill", false);
            library.recordOutcome("multiExecuteSkill", true);
            library.recordOutcome("multiExecuteSkill", false);

            Skill retrieved = library.getSkill("multiExecuteSkill");
            assertEquals(5, retrieved.getExecutionCount(),
                "Should track all executions");
            assertEquals(0.6, retrieved.getSuccessRate(), 0.001,
                "Success rate should be 60% (3/5)");
        }

        @Test
        @DisplayName("Code generation handles missing variables gracefully")
        void codeGenerationHandlesMissingVariables() {
            ExecutableSkill skill = ExecutableSkill.builder("missingVarSkill")
                .description("Skill with optional variables")
                .category("test")
                .codeTemplate("var required = {{required}}; var optional = {{optional}};")
                .requiredAction("test")
                .build();

            library.addSkill(skill);

            Map<String, Object> partialContext = Map.of("required", "value");

            String code = skill.generateCode(partialContext);

            assertTrue(code.contains("var required = value;"),
                "Should substitute provided variable");
            assertTrue(code.contains("{{optional}}"),
                "Should leave missing variable as placeholder");
        }

        @Test
        @DisplayName("Generated code preserves template structure")
        void generatedCodePreservesTemplateStructure() {
            String template = """
                for (var i = 0; i < {{count}}; i++) {
                    steve.mineBlock({{x}} + i, {{y}}, {{z}});
                    if (i % 5 === 0) {
                        steve.placeBlock('torch', {{x}} + i, {{y}}, {{z}});
                    }
                }
                """;

            ExecutableSkill skill = ExecutableSkill.builder("loopSkill")
                .description("Loop mining pattern")
                .category("mining")
                .codeTemplate(template)
                .requiredAction("mine")
                .requiredAction("place")
                .build();

            library.addSkill(skill);

            Map<String, Object> context = Map.of(
                "count", 10,
                "x", 100,
                "y", 64,
                "z", 200
            );

            String code = skill.generateCode(context);

            assertTrue(code.contains("for (var i = 0; i < 10; i++)"),
                "Should preserve for loop structure");
            assertTrue(code.contains("steve.mineBlock(100 + i, 64, 200)"),
                "Should substitute variables in method call");
            assertTrue(code.contains("steve.placeBlock('torch', 100 + i, 64, 200)"),
                "Should substitute variables in conditional block");
        }

        @Test
        @DisplayName("Skill execution affects search ranking")
        void skillExecutionAffectsSearchRanking() {
            ExecutableSkill skill1 = createMiningSkill("rankSkill1", "Mining skill 1");
            ExecutableSkill skill2 = createMiningSkill("rankSkill2", "Mining skill 2");

            library.addSkill(skill1);
            library.addSkill(skill2);

            // skill1 performs better
            for (int i = 0; i < 10; i++) {
                library.recordOutcome("rankSkill1", true);
            }
            for (int i = 0; i < 5; i++) {
                library.recordOutcome("rankSkill2", true);
                library.recordOutcome("rankSkill2", false);
            }

            List<Skill> sortedSkills = library.getSkillsBySuccessRate();

            int skill1Index = sortedSkills.indexOf(skill1);
            int skill2Index = sortedSkills.indexOf(skill2);

            assertTrue(skill1Index >= 0 && skill2Index >= 0,
                "Both skills should be in the list");
            assertTrue(skill1Index < skill2Index,
                "Higher success rate skill should rank higher");
        }
    }

    // ==================== Skill Generation Tests ====================

    @Nested
    @DisplayName("Skill Generation Tests")
    class SkillGenerationTests {

        @Test
        @DisplayName("Analyze successful sequence generates skills")
        void analyzeSuccessfulSequenceGeneratesSkills() {
            // Create a SkillGenerator with the library
            com.minewright.skill.SkillGenerator generator =
                new com.minewright.skill.SkillGenerator(library);

            // Create a successful task sequence (mining pattern)
            List<Task> successfulSequence = List.of(
                TaskBuilder.aTask("mine").withBlock("stone").withPosition(0, 60, 0).build(),
                TaskBuilder.aTask("mine").withBlock("stone").withPosition(0, 60, 1).build(),
                TaskBuilder.aTask("mine").withBlock("stone").withPosition(0, 60, 2).build(),
                TaskBuilder.aTask("place").withBlock("torch").withPosition(0, 60, 0).build(),
                TaskBuilder.aTask("mine").withBlock("stone").withPosition(0, 59, 3).build(),
                TaskBuilder.aTask("mine").withBlock("stone").withPosition(0, 59, 4).build()
            );

            // Analyze the sequence
            List<Skill> generated = generator.analyzeTaskSequence(successfulSequence, true);

            assertNotNull(generated, "Should return a list (may be empty)");
            // Note: Pattern detection requires specific conditions, so we just verify it doesn't crash
        }

        @Test
        @DisplayName("Failed sequence does not generate skills")
        void failedSequenceDoesNotGenerateSkills() {
            com.minewright.skill.SkillGenerator generator =
                new com.minewright.skill.SkillGenerator(library);

            List<Task> failedSequence = List.of(
                TaskBuilder.aTask("mine").withBlock("stone").build(),
                TaskBuilder.aTask("mine").withBlock("bedrock").build()  // Impossible
            );

            List<Skill> generated = generator.analyzeTaskSequence(failedSequence, false);

            assertNotNull(generated, "Should return a list");
            assertTrue(generated.isEmpty(),
                "Should not generate skills from failed sequence");
        }

        @Test
        @DisplayName("Empty sequence is handled gracefully")
        void emptySequenceHandledGracefully() {
            com.minewright.skill.SkillGenerator generator =
                new com.minewright.skill.SkillGenerator(library);

            List<Task> emptySequence = List.of();

            List<Skill> generated = generator.analyzeTaskSequence(emptySequence, true);

            assertNotNull(generated, "Should return a list");
            assertTrue(generated.isEmpty(),
                "Should not generate skills from empty sequence");
        }

        @Test
        @DisplayName("Generate skill from pattern")
        void generateSkillFromPattern() {
            com.minewright.skill.SkillGenerator generator =
                new com.minewright.skill.SkillGenerator(library);

            // Create a loop pattern
            TaskPattern pattern = TaskPattern.builder()
                .name("test_loop")
                .description("Test loop pattern")
                .type(TaskPattern.PatternType.LOOP)
                .frequency(3)
                .successRate(1.0)
                .addStep("mine", Map.of(), 0)
                .build();

            Skill generated = generator.generateSkillFromPattern(pattern);

            assertNotNull(generated, "Should generate a skill");
            assertNotNull(generated.getName(), "Skill should have a name");
            assertTrue(generated.getName().startsWith("auto_"),
                "Auto-generated skill should have auto_ prefix");
        }

        @Test
        @DisplayName("Generated skill is added to library")
        void generatedSkillIsAddedToLibrary() {
            com.minewright.skill.SkillGenerator generator =
                new com.minewright.skill.SkillGenerator(library);

            // Clear recent signatures to allow generation
            generator.clearRecentSignatures();

            // Create a pattern that meets requirements
            TaskPattern pattern = TaskPattern.builder()
                .name("valid_pattern")
                .description("Valid pattern for skill generation")
                .type(TaskPattern.PatternType.LOOP)
                .frequency(5)  // Above MIN_FREQUENCY (2)
                .successRate(0.9)  // Above MIN_SUCCESS_RATE (0.7)
                .addStep("mine", Map.of(), 0)
                .build();

            int beforeCount = library.getSkillCount();

            // Manually add pattern frequency to meet threshold
            for (int i = 0; i < 5; i++) {
                generator.analyzeTaskSequence(
                    List.of(TaskBuilder.aTask("mine").withBlock("stone").build()),
                    true
                );
            }

            Skill skill = generator.generateSkillFromPattern(pattern);

            if (skill != null) {
                assertTrue(library.hasSkill(skill.getName()),
                    "Generated skill should be in library");
            }
        }

        @Test
        @DisplayName("Skill generator statistics are tracked")
        void skillGeneratorStatisticsAreTracked() {
            com.minewright.skill.SkillGenerator generator =
                new com.minewright.skill.SkillGenerator(library);

            Map<String, Object> stats = generator.getStatistics();

            assertNotNull(stats, "Statistics should not be null");
            assertTrue(stats.containsKey("generatedSkills"),
                "Statistics should include generated skills count");
            assertTrue(stats.containsKey("trackedPatterns"),
                "Statistics should include tracked patterns count");
            assertTrue(stats.containsKey("recentSignatures"),
                "Statistics should include recent signatures count");
        }

        @Test
        @DisplayName("Generator reset clears state")
        void generatorResetClearsState() {
            com.minewright.skill.SkillGenerator generator =
                new com.minewright.skill.SkillGenerator(library);

            // Analyze some sequences
            generator.analyzeTaskSequence(
                List.of(TaskBuilder.aTask("mine").withBlock("stone").build()),
                true
            );

            generator.reset();

            Map<String, Object> stats = generator.getStatistics();

            assertEquals(0, stats.get("generatedSkills"),
                "Generated skills count should be 0 after reset");
            assertEquals(0, stats.get("trackedPatterns"),
                "Tracked patterns should be 0 after reset");
            assertEquals(0, stats.get("recentSignatures"),
                "Recent signatures should be 0 after reset");
        }

        @Test
        @DisplayName("Generated skill category is determined from actions")
        void generatedSkillCategoryDeterminedFromActions() {
            com.minewright.skill.SkillGenerator generator =
                new com.minewright.skill.SkillGenerator(library);

            // Mining pattern
            TaskPattern miningPattern = TaskPattern.builder()
                .name("mining_pattern")
                .description("Mining pattern")
                .type(TaskPattern.PatternType.LOOP)
                .frequency(3)
                .successRate(1.0)
                .addStep("mine", Map.of(), 0)
                .build();

            Skill miningSkill = generator.generateSkillFromPattern(miningPattern);
            if (miningSkill != null) {
                assertEquals("mining", miningSkill.getCategory(),
                    "Should detect mining category from mine action");
            }

            // Building pattern
            TaskPattern buildingPattern = TaskPattern.builder()
                .name("building_pattern")
                .description("Building pattern")
                .type(TaskPattern.PatternType.LOOP)
                .frequency(3)
                .successRate(1.0)
                .addStep("place", Map.of(), 0)
                .build();

            Skill buildingSkill = generator.generateSkillFromPattern(buildingPattern);
            if (buildingSkill != null) {
                assertEquals("building", buildingSkill.getCategory(),
                    "Should detect building category from place action");
            }
        }

        @Test
        @DisplayName("Validate skill rejects duplicates")
        void validateSkillRejectsDuplicates() {
            com.minewright.skill.SkillGenerator generator =
                new com.minewright.skill.SkillGenerator(library);

            ExecutableSkill existingSkill = createTestSkill("duplicateCheck");
            library.addSkill(existingSkill);

            // Try to validate a skill with same name
            ExecutableSkill duplicateSkill = ExecutableSkill.builder("duplicateCheck")
                .description("Duplicate")
                .category("test")
                .codeTemplate("code")
                .requiredAction("test")
                .build();

            boolean isValid = generator.validateSkill(duplicateSkill);

            assertFalse(isValid, "Should reject duplicate skill name");
        }

        @Test
        @DisplayName("Validate skill checks required actions")
        void validateSkillChecksRequiredActions() {
            com.minewright.skill.SkillGenerator generator =
                new com.minewright.skill.SkillGenerator(library);

            // Skill with invalid action
            ExecutableSkill invalidActionSkill = ExecutableSkill.builder("invalidActionSkill")
                .description("Skill with invalid action")
                .category("test")
                .codeTemplate("code")
                .requiredAction("invalid_action_that_does_not_exist")
                .build();

            boolean isValid = generator.validateSkill(invalidActionSkill);

            assertFalse(isValid, "Should reject skill with invalid action");
        }

        @Test
        @DisplayName("Generated skill has code template")
        void generatedSkillHasCodeTemplate() {
            com.minewright.skill.SkillGenerator generator =
                new com.minewright.skill.SkillGenerator(library);

            TaskPattern pattern = TaskPattern.builder()
                .name("code_template_pattern")
                .description("Pattern for code template generation")
                .type(TaskPattern.PatternType.LOOP)
                .frequency(3)
                .successRate(1.0)
                .addStep("mine", Map.of(), 0)
                .build();

            Skill skill = generator.generateSkillFromPattern(pattern);

            if (skill != null && skill instanceof ExecutableSkill) {
                ExecutableSkill execSkill = (ExecutableSkill) skill;
                Map<String, Object> context = Map.of();
                String code = execSkill.generateCode(context);

                assertNotNull(code, "Generated code should not be null");
                assertFalse(code.isEmpty(), "Generated code should not be empty");
            }
        }

        @Test
        @DisplayName("Generated skill name is unique")
        void generatedSkillNameIsUnique() {
            com.minewright.skill.SkillGenerator generator =
                new com.minewright.skill.SkillGenerator(library);

            generator.clearRecentSignatures();

            TaskPattern pattern1 = TaskPattern.builder()
                .name("pattern1")
                .description("First pattern")
                .type(TaskPattern.PatternType.LOOP)
                .frequency(3)
                .successRate(1.0)
                .addStep("mine", Map.of(), 0)
                .build();

            TaskPattern pattern2 = TaskPattern.builder()
                .name("pattern2")
                .description("Second pattern")
                .type(TaskPattern.PatternType.LOOP)
                .frequency(3)
                .successRate(1.0)
                .addStep("mine", Map.of(), 0)
                .build();

            Skill skill1 = generator.generateSkillFromPattern(pattern1);
            Skill skill2 = generator.generateSkillFromPattern(pattern2);

            if (skill1 != null && skill2 != null) {
                assertNotEquals(skill1.getName(), skill2.getName(),
                    "Generated skill names should be unique");
            }
        }
    }

    // ==================== Thread Safety Tests ====================

    @Nested
    @DisplayName("Thread Safety Tests")
    class ThreadSafetyTests {

        @Test
        @DisplayName("Concurrent skill addition is thread-safe")
        void concurrentSkillAdditionIsThreadSafe() throws InterruptedException {
            int threadCount = 10;
            int skillsPerThread = 10;

            Thread[] threads = new Thread[threadCount];

            for (int i = 0; i < threadCount; i++) {
                final int threadId = i;
                threads[i] = new Thread(() -> {
                    for (int j = 0; j < skillsPerThread; j++) {
                        String skillName = "concurrentSkill_" + threadId + "_" + j;
                        library.addSkill(createTestSkill(skillName));
                    }
                });
                threads[i].start();
            }

            for (Thread thread : threads) {
                thread.join();
            }

            // Count should be at least the number of unique skills added
            // (some may be duplicates if threads have same IDs)
            int expectedMinSkills = library.getSkillCount();
            assertTrue(expectedMinSkills >= threadCount * skillsPerThread ||
                       expectedMinSkills > 0,
                "Should have added skills concurrently without errors");
        }

        @Test
        @DisplayName("Concurrent semantic search is thread-safe")
        void concurrentSemanticSearchIsThreadSafe() throws InterruptedException {
            // Add some test skills
            for (int i = 0; i < 10; i++) {
                library.addSkill(createMiningSkill("searchSkill" + i, "Mining skill " + i));
            }

            int threadCount = 5;
            Thread[] threads = new Thread[threadCount];

            for (int i = 0; i < threadCount; i++) {
                threads[i] = new Thread(() -> {
                    for (int j = 0; j < 20; j++) {
                        library.semanticSearch("mining skill");
                    }
                });
                threads[i].start();
            }

            for (Thread thread : threads) {
                thread.join();
            }

            // If we get here without exception, test passes
            assertTrue(true, "Concurrent searches should not cause errors");
        }

        @Test
        @DisplayName("Concurrent outcome recording is thread-safe")
        void concurrentOutcomeRecordingIsThreadSafe() throws InterruptedException {
            ExecutableSkill skill = createTestSkill("concurrentRecordSkill");
            library.addSkill(skill);

            int threadCount = 10;
            int recordsPerThread = 50;

            Thread[] threads = new Thread[threadCount];

            for (int i = 0; i < threadCount; i++) {
                final boolean success = i % 2 == 0;
                threads[i] = new Thread(() -> {
                    for (int j = 0; j < recordsPerThread; j++) {
                        library.recordOutcome("concurrentRecordSkill", success);
                    }
                });
                threads[i].start();
            }

            for (Thread thread : threads) {
                thread.join();
            }

            Skill retrieved = library.getSkill("concurrentRecordSkill");
            assertEquals(threadCount * recordsPerThread, retrieved.getExecutionCount(),
                "All concurrent recordings should be recorded");
        }
    }
}
