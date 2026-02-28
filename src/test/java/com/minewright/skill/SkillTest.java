package com.minewright.skill;

import com.minewright.action.Task;
import com.minewright.testutil.TaskBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Skill} interface and {@link ExecutableSkill} implementation.
 *
 * <p>Tests cover:</p>
 * <ul>
 *   <li>Skill metadata (name, description, category)</li>
 *   <li>Code generation with template substitution</li>
 *   <li>Success rate tracking</li>
 *   <li>Task applicability matching</li>
 *   <li>Builder pattern validation</li>
 *   <li>Thread-safe execution counting</li>
 * </ul>
 */
@DisplayName("Skill Tests")
class SkillTest {

    private ExecutableSkill testSkill;
    private static final String TEST_SKILL_NAME = "testSkill";
    private static final String TEST_DESCRIPTION = "A test skill for unit testing";

    @BeforeEach
    void setUp() {
        testSkill = createBasicTestSkill();
    }

    // ==================== Factory Methods ====================

    private ExecutableSkill createBasicTestSkill() {
        return ExecutableSkill.builder(TEST_SKILL_NAME)
            .description(TEST_DESCRIPTION)
            .category("test")
            .codeTemplate("var depth = {{depth}}; var direction = {{direction:quote}};")
            .requiredAction("mine")
            .requiredAction("place")
            .build();
    }

    private ExecutableSkill createSkillWithPattern(String pattern) {
        return ExecutableSkill.builder("patternSkill")
            .description("Skill with applicability pattern")
            .category("test")
            .codeTemplate("console.log('{{action:quote}}');")
            .requiredAction("test")
            .applicabilityPattern(pattern)
            .build();
    }

    // ==================== Required Fields Tests ====================

    @Nested
    @DisplayName("Required Fields Tests")
    class RequiredFieldsTests {

        @Test
        @DisplayName("Skill has required name field")
        void skillHasRequiredNameField() {
            assertEquals(TEST_SKILL_NAME, testSkill.getName(),
                "Skill name should match the provided name");
        }

        @Test
        @DisplayName("Skill has required description field")
        void skillHasRequiredDescriptionField() {
            assertEquals(TEST_DESCRIPTION, testSkill.getDescription(),
                "Skill description should match the provided description");
        }

        @Test
        @DisplayName("Skill has category field")
        void skillHasCategoryField() {
            assertEquals("test", testSkill.getCategory(),
                "Skill category should match the provided category");
        }

        @Test
        @DisplayName("Builder throws exception for empty name")
        void builderThrowsExceptionForEmptyName() {
            assertThrows(IllegalArgumentException.class,
                () -> ExecutableSkill.builder("").description("test").codeTemplate("code").build(),
                "Builder should throw exception for empty name");
        }

        @Test
        @DisplayName("Builder throws exception for null name")
        void builderThrowsExceptionForNullName() {
            assertThrows(IllegalArgumentException.class,
                () -> ExecutableSkill.builder(null).description("test").codeTemplate("code").build(),
                "Builder should throw exception for null name");
        }

        @Test
        @DisplayName("Builder throws exception for empty description")
        void builderThrowsExceptionForEmptyDescription() {
            assertThrows(IllegalArgumentException.class,
                () -> ExecutableSkill.builder("test").description("").codeTemplate("code").build(),
                "Builder should throw exception for empty description");
        }

        @Test
        @DisplayName("Builder throws exception for empty code template")
        void builderThrowsExceptionForEmptyCodeTemplate() {
            assertThrows(IllegalArgumentException.class,
                () -> ExecutableSkill.builder("test").description("desc").codeTemplate("").build(),
                "Builder should throw exception for empty code template");
        }

        @Test
        @DisplayName("Builder throws exception for null code template")
        void builderThrowsExceptionForNullCodeTemplate() {
            assertThrows(IllegalArgumentException.class,
                () -> ExecutableSkill.builder("test").description("desc").codeTemplate(null).build(),
                "Builder should throw exception for null code template");
        }

        @Test
        @DisplayName("Default category is utility when not specified")
        void defaultCategoryIsUtility() {
            ExecutableSkill skill = ExecutableSkill.builder("noCategorySkill")
                .description("test")
                .codeTemplate("code")
                .build();

            assertEquals("utility", skill.getCategory(),
                "Default category should be 'utility' when not specified");
        }

        @Test
        @DisplayName("Skill has required actions list")
        void skillHasRequiredActionsList() {
            List<String> actions = testSkill.getRequiredActions();

            assertNotNull(actions, "Required actions list should not be null");
            assertEquals(2, actions.size(), "Should have 2 required actions");
            assertTrue(actions.contains("mine"), "Should contain 'mine' action");
            assertTrue(actions.contains("place"), "Should contain 'place' action");
        }

        @Test
        @DisplayName("Required actions list is immutable")
        void requiredActionsListIsImmutable() {
            List<String> actions = testSkill.getRequiredActions();

            assertThrows(UnsupportedOperationException.class,
                () -> actions.add("newAction"),
                "Required actions list should be immutable");
        }

        @Test
        @DisplayName("Skill has required items list")
        void skillHasRequiredItemsList() {
            ExecutableSkill skill = ExecutableSkill.builder("itemSkill")
                .description("test")
                .codeTemplate("code")
                .requiredItem("pickaxe")
                .requiredItem("torch")
                .build();

            List<String> items = skill.getRequiredItems();

            assertNotNull(items, "Required items list should not be null");
            assertEquals(2, items.size(), "Should have 2 required items");
            assertTrue(items.contains("pickaxe"), "Should contain 'pickaxe' item");
            assertTrue(items.contains("torch"), "Should contain 'torch' item");
        }

        @Test
        @DisplayName("Default required items is empty list")
        void defaultRequiredItemsIsEmptyList() {
            List<String> items = testSkill.getRequiredItems();

            assertNotNull(items, "Required items should not be null");
            assertTrue(items.isEmpty(), "Default required items should be empty");
        }

        @Test
        @DisplayName("Skill has estimated ticks")
        void skillHasEstimatedTicks() {
            ExecutableSkill skill = ExecutableSkill.builder("tickSkill")
                .description("test")
                .codeTemplate("code")
                .estimatedTicks(250)
                .build();

            assertEquals(250, skill.getEstimatedTicks(),
                "Estimated ticks should match the provided value");
        }

        @Test
        @DisplayName("Default estimated ticks is 100")
        void defaultEstimatedTicksIs100() {
            assertEquals(100, testSkill.getEstimatedTicks(),
                "Default estimated ticks should be 100");
        }
    }

    // ==================== Code Generation Tests ====================

    @Nested
    @DisplayName("Code Generation Tests")
    class CodeGenerationTests {

        @Test
        @DisplayName("Skill generates valid code with context substitution")
        void skillGeneratesValidCodeWithContext() {
            Map<String, Object> context = new HashMap<>();
            context.put("depth", 10);
            context.put("direction", "north");

            String generatedCode = testSkill.generateCode(context);

            assertNotNull(generatedCode, "Generated code should not be null");
            assertTrue(generatedCode.contains("var depth = 10;"),
                "Code should contain substituted depth value");
            assertTrue(generatedCode.contains("var direction = \"north\";"),
                "Code should contain quoted direction value");
        }

        @Test
        @DisplayName("Code template substitutes all variables")
        void codeTemplateSubstitutesAllVariables() {
            ExecutableSkill skill = ExecutableSkill.builder("multiVarSkill")
                .description("test")
                .codeTemplate("{{a}} + {{b}} = {{c}}")
                .build();

            Map<String, Object> context = new HashMap<>();
            context.put("a", 1);
            context.put("b", 2);
            context.put("c", 3);

            String code = skill.generateCode(context);

            assertEquals("1 + 2 = 3", code, "All variables should be substituted");
        }

        @Test
        @DisplayName("Quoted substitution wraps values in quotes")
        void quotedSubstitutionWrapsValuesInQuotes() {
            Map<String, Object> context = new HashMap<>();
            context.put("direction", "south");

            String code = testSkill.generateCode(context);

            assertTrue(code.contains("\"south\""), "Quoted variable should be wrapped in quotes");
            assertFalse(code.contains("{{direction:quote}}"), "Placeholder should be replaced");
        }

        @Test
        @DisplayName("Null context values become null in code")
        void nullContextValuesBecomeNullInCode() {
            ExecutableSkill skill = ExecutableSkill.builder("nullSkill")
                .description("test")
                .codeTemplate("var x = {{value}};")
                .build();

            Map<String, Object> context = new HashMap<>();
            context.put("value", null);

            String code = skill.generateCode(context);

            assertTrue(code.contains("var x = null;"), "Null values should become 'null' in code");
        }

        @Test
        @DisplayName("Code template handles numeric values")
        void codeTemplateHandlesNumericValues() {
            ExecutableSkill skill = ExecutableSkill.builder("numericSkill")
                .description("test")
                .codeTemplate("{{integer}} + {{double}} + {{long}}")
                .build();

            Map<String, Object> context = new HashMap<>();
            context.put("integer", 42);
            context.put("double", 3.14);
            context.put("long", 9999999999L);

            String code = skill.generateCode(context);

            assertTrue(code.contains("42"), "Integer should be substituted");
            assertTrue(code.contains("3.14"), "Double should be substituted");
            assertTrue(code.contains("9999999999"), "Long should be substituted");
        }

        @Test
        @DisplayName("Missing context variables leave placeholder intact")
        void missingContextVariablesLeavePlaceholderIntact() {
            Map<String, Object> context = new HashMap<>();
            context.put("depth", 5);
            // direction is missing

            String code = testSkill.generateCode(context);

            assertTrue(code.contains("var depth = 5;"), "Present variable should be substituted");
            assertTrue(code.contains("{{direction:quote}}"),
                "Missing variable should leave placeholder intact");
        }

        @Test
        @DisplayName("Code generation preserves non-placeholder text")
        void codeGenerationPreservesNonPlaceholderText() {
            ExecutableSkill skill = ExecutableSkill.builder("preserveSkill")
                .description("test")
                .codeTemplate("for (var i = 0; i < {{max}}; i++) { doSomething(); }")
                .build();

            Map<String, Object> context = new HashMap<>();
            context.put("max", 10);

            String code = skill.generateCode(context);

            assertTrue(code.contains("for (var i = 0; i < 10; i++)"),
                "Non-placeholder text should be preserved");
            assertTrue(code.contains("{ doSomething(); }"),
                "Code structure should be preserved");
        }

        @Test
        @DisplayName("Code generation handles special characters in values")
        void codeGenerationHandlesSpecialCharactersInValues() {
            ExecutableSkill skill = ExecutableSkill.builder("specialCharSkill")
                .description("test")
                .codeTemplate("var s = {{text:quote}};")
                .build();

            Map<String, Object> context = new HashMap<>();
            context.put("text", "hello's \"world\"");

            String code = skill.generateCode(context);

            assertTrue(code.contains("\"hello's \\\"world\\\"\""),
                "Special characters should be preserved in quoted strings");
        }
    }

    // ==================== Success Rate Tests ====================

    @Nested
    @DisplayName("Success Rate Tests")
    class SuccessRateTests {

        @Test
        @DisplayName("New skill has 100% success rate (untested)")
        void newSkillHasDefaultSuccessRate() {
            assertEquals(1.0, testSkill.getSuccessRate(), 0.001,
                "New skill should have 100% success rate");
        }

        @Test
        @DisplayName("Success rate is 0.0 after one failure")
        void successRateIsZeroAfterOneFailure() {
            testSkill.recordSuccess(false);

            assertEquals(0.0, testSkill.getSuccessRate(), 0.001,
                "Success rate should be 0.0 after one failure");
        }

        @Test
        @DisplayName("Success rate is 1.0 after one success")
        void successRateIsOneAfterOneSuccess() {
            testSkill.recordSuccess(true);

            assertEquals(1.0, testSkill.getSuccessRate(), 0.001,
                "Success rate should be 1.0 after one success");
        }

        @Test
        @DisplayName("Success rate calculates correctly for mixed results")
        void successRateCalculatesCorrectlyForMixedResults() {
            // 3 successes, 2 failures = 60%
            testSkill.recordSuccess(true);
            testSkill.recordSuccess(true);
            testSkill.recordSuccess(true);
            testSkill.recordSuccess(false);
            testSkill.recordSuccess(false);

            assertEquals(0.6, testSkill.getSuccessRate(), 0.001,
                "Success rate should be 0.6 for 3/5 successes");
        }

        @Test
        @DisplayName("Success rate updates incrementally")
        void successRateUpdatesIncrementally() {
            testSkill.recordSuccess(true);
            assertEquals(1.0, testSkill.getSuccessRate(), 0.001);

            testSkill.recordSuccess(false);
            assertEquals(0.5, testSkill.getSuccessRate(), 0.001);

            testSkill.recordSuccess(true);
            assertEquals(0.666, testSkill.getSuccessRate(), 0.001);
        }

        @Test
        @DisplayName("Execution count increments on each record")
        void executionCountIncrementsOnEachRecord() {
            assertEquals(0, testSkill.getExecutionCount(),
                "Initial execution count should be 0");

            testSkill.recordSuccess(true);
            assertEquals(1, testSkill.getExecutionCount());

            testSkill.recordSuccess(false);
            assertEquals(2, testSkill.getExecutionCount());

            testSkill.recordSuccess(true);
            assertEquals(3, testSkill.getExecutionCount());
        }

        @Test
        @DisplayName("Success rate is bounded between 0 and 1")
        void successRateIsBounded() {
            // All failures
            for (int i = 0; i < 10; i++) {
                testSkill.recordSuccess(false);
            }

            assertTrue(testSkill.getSuccessRate() >= 0.0,
                "Success rate should not be negative");
            assertTrue(testSkill.getSuccessRate() <= 1.0,
                "Success rate should not exceed 1.0");

            // Reset and all successes
            testSkill = createBasicTestSkill();
            for (int i = 0; i < 10; i++) {
                testSkill.recordSuccess(true);
            }

            assertEquals(1.0, testSkill.getSuccessRate(), 0.001,
                "Success rate should be exactly 1.0 for all successes");
        }
    }

    // ==================== Applicability Tests ====================

    @Nested
    @DisplayName("Task Applicability Tests")
    class ApplicabilityTests {

        @Test
        @DisplayName("Skill is applicable to matching action type")
        void skillIsApplicableToMatchingAction() {
            Task task = TaskBuilder.aTask("mine")
                .withBlock("stone")
                .build();

            assertTrue(testSkill.isApplicable(task),
                "Skill should be applicable to tasks with matching action type");
        }

        @Test
        @DisplayName("Skill is not applicable to non-matching action type")
        void skillIsNotApplicableToNonMatchingAction() {
            Task task = TaskBuilder.aTask("craft")
                .withItem("sword")
                .build();

            assertFalse(testSkill.isApplicable(task),
                "Skill should not be applicable to tasks with different action type");
        }

        @Test
        @DisplayName("Pattern matching determines applicability")
        void patternMatchingDeterminesApplicability() {
            ExecutableSkill skill = createSkillWithPattern("dig.*staircase|mining.*stair");

            Task matchingTask = TaskBuilder.aTask("mine")
                .withParam("description", "dig staircase for mining")
                .build();

            Task nonMatchingTask = TaskBuilder.aTask("mine")
                .withParam("description", "mine some stone")
                .build();

            assertTrue(skill.isApplicable(matchingTask),
                "Pattern should match task description containing keywords");
            assertFalse(skill.isApplicable(nonMatchingTask),
                "Pattern should not match task without keywords");
        }

        @Test
        @DisplayName("Pattern matching is case insensitive")
        void patternMatchingIsCaseInsensitive() {
            ExecutableSkill skill = createSkillWithPattern("build.*shelter");

            Task upperCaseTask = TaskBuilder.aTask("build")
                .withParam("type", "SHELTER")
                .build();

            Task mixedCaseTask = TaskBuilder.aTask("build")
                .withParam("type", "ShElTeR")
                .build();

            assertTrue(skill.isApplicable(upperCaseTask),
                "Pattern matching should be case insensitive");
            assertTrue(skill.isApplicable(mixedCaseTask),
                "Pattern matching should work with mixed case");
        }

        @Test
        @DisplayName("Task toString is used for pattern matching")
        void taskToStringIsUsedForPatternMatching() {
            ExecutableSkill skill = createSkillWithPattern("stone.*mine|mine.*stone");

            Task task = TaskBuilder.aTask("mine")
                .withBlock("stone")
                .withQuantity(10)
                .build();

            // Task.toString() returns "Task{action='mine', parameters={block=stone, quantity=10}}"
            assertTrue(skill.isApplicable(task),
                "Pattern should match against task's toString representation");
        }

        @Test
        @DisplayName("Pattern takes precedence over action type check")
        void patternTakesPrecedenceOverActionCheck() {
            ExecutableSkill skill = createSkillWithPattern("special.*pattern");

            Task taskWithMatchingAction = TaskBuilder.aTask("test")
                .withParam("description", "normal test")
                .build();

            Task taskWithMatchingPattern = TaskBuilder.aTask("other")
                .withParam("description", "special pattern match")
                .build();

            assertFalse(skill.isApplicable(taskWithMatchingAction),
                "Should not match by action type when pattern is defined");
            assertTrue(skill.isApplicable(taskWithMatchingPattern),
                "Should match by pattern even with different action");
        }

        @Test
        @DisplayName("Skill without pattern matches by action type")
        void skillWithoutPatternMatchesByActionType() {
            ExecutableSkill skill = ExecutableSkill.builder("noPatternSkill")
                .description("Skill without pattern")
                .codeTemplate("code")
                .requiredAction("mine")
                .build();

            Task mineTask = TaskBuilder.aTask("mine").withBlock("coal").build();
            Task placeTask = TaskBuilder.aTask("place").withBlock("stone").build();

            assertTrue(skill.isApplicable(mineTask),
                "Should match by action type when no pattern is defined");
            assertFalse(skill.isApplicable(placeTask),
                "Should not match different action type");
        }
    }

    // ==================== Builder Tests ====================

    @Nested
    @DisplayName("Builder Pattern Tests")
    class BuilderTests {

        @Test
        @DisplayName("Builder creates skill with all specified properties")
        void builderCreatesSkillWithAllProperties() {
            ExecutableSkill skill = ExecutableSkill.builder("fullSkill")
                .description("Full featured skill")
                .category("mining")
                .codeTemplate("var x = {{value}};")
                .requiredAction("mine")
                .requiredAction("place")
                .requiredItem("pickaxe")
                .requiredItem("torch")
                .estimatedTicks(500)
                .applicabilityPattern("mine.*ore")
                .build();

            assertEquals("fullSkill", skill.getName());
            assertEquals("Full featured skill", skill.getDescription());
            assertEquals("mining", skill.getCategory());
            assertEquals(2, skill.getRequiredActions().size());
            assertEquals(2, skill.getRequiredItems().size());
            assertEquals(500, skill.getEstimatedTicks());
        }

        @Test
        @DisplayName("Builder supports fluent chaining")
        void builderSupportsFluentChaining() {
            ExecutableSkill skill = ExecutableSkill.builder("chainSkill")
                .description("test")
                .category("test")
                .codeTemplate("code")
                .requiredAction("action1")
                .requiredAction("action2")
                .requiredItem("item1")
                .estimatedTicks(200)
                .build();

            assertNotNull(skill, "Builder should support fluent chaining");
            assertEquals("chainSkill", skill.getName());
        }

        @Test
        @DisplayName("Builder handles multiple actions at once")
        void builderHandlesMultipleActionsAtOnce() {
            ExecutableSkill skill = ExecutableSkill.builder("multiActionSkill")
                .description("test")
                .codeTemplate("code")
                .requiredActions("mine", "place", "craft", "move")
                .build();

            assertEquals(4, skill.getRequiredActions().size(),
                "Should add all actions at once");
            assertTrue(skill.getRequiredActions().contains("craft"));
            assertTrue(skill.getRequiredActions().contains("move"));
        }

        @Test
        @DisplayName("Builder handles multiple items at once")
        void builderHandlesMultipleItemsAtOnce() {
            ExecutableSkill skill = ExecutableSkill.builder("multiItemSkill")
                .description("test")
                .codeTemplate("code")
                .requiredItems("pickaxe", "torch", "food", "water")
                .build();

            assertEquals(4, skill.getRequiredItems().size(),
                "Should add all items at once");
        }

        @Test
        @DisplayName("Builder creates independent skill instances")
        void builderCreatesIndependentInstances() {
            ExecutableSkill skill1 = ExecutableSkill.builder("skill1")
                .description("First skill")
                .codeTemplate("code1")
                .build();

            ExecutableSkill skill2 = ExecutableSkill.builder("skill2")
                .description("Second skill")
                .codeTemplate("code2")
                .build();

            assertNotEquals(skill1.getName(), skill2.getName());
            assertNotEquals(skill1.getDescription(), skill2.getDescription());

            // Verify independence
            skill1.recordSuccess(true);
            assertEquals(0, skill2.getExecutionCount(),
                "Skills should have independent execution counts");
        }
    }

    // ==================== ToString Tests ====================

    @Nested
    @DisplayName("ToString Tests")
    class ToStringTests {

        @Test
        @DisplayName("ToString contains skill name")
        void toStringContainsSkillName() {
            String result = testSkill.toString();

            assertTrue(result.contains(TEST_SKILL_NAME),
                "toString should contain skill name");
        }

        @Test
        @DisplayName("ToString contains category")
        void toStringContainsCategory() {
            String result = testSkill.toString();

            assertTrue(result.contains("test"),
                "toString should contain category");
        }

        @Test
        @DisplayName("ToString contains success rate")
        void toStringContainsSuccessRate() {
            testSkill.recordSuccess(true);
            testSkill.recordSuccess(false);

            String result = testSkill.toString();

            assertTrue(result.contains("50"),
                "toString should contain success rate percentage");
        }

        @Test
        @DisplayName("ToString contains execution count")
        void toStringContainsExecutionCount() {
            testSkill.recordSuccess(true);
            testSkill.recordSuccess(true);

            String result = testSkill.toString();

            assertTrue(result.contains("2"),
                "toString should contain execution count");
        }

        @Test
        @DisplayName("ToString follows expected format")
        void toStringFollowsExpectedFormat() {
            String result = testSkill.toString();

            assertTrue(result.startsWith("Skill["),
                "toString should start with 'Skill['");
            assertTrue(result.endsWith("]"),
                "toString should end with ']'");
        }
    }

    // ==================== Thread Safety Tests ====================

    @Nested
    @DisplayName("Thread Safety Tests")
    class ThreadSafetyTests {

        @Test
        @DisplayName("Concurrent success recording is thread-safe")
        void concurrentSuccessRecordingIsThreadSafe() throws InterruptedException {
            int threadCount = 10;
            int recordsPerThread = 100;

            Thread[] threads = new Thread[threadCount];

            for (int i = 0; i < threadCount; i++) {
                final boolean success = i % 2 == 0;
                threads[i] = new Thread(() -> {
                    for (int j = 0; j < recordsPerThread; j++) {
                        testSkill.recordSuccess(success);
                    }
                });
                threads[i].start();
            }

            for (Thread thread : threads) {
                thread.join();
            }

            assertEquals(threadCount * recordsPerThread, testSkill.getExecutionCount(),
                "All executions should be recorded without race conditions");
        }

        @Test
        @DisplayName("Execution count is consistent under concurrent access")
        void executionCountIsConsistentUnderConcurrentAccess() throws InterruptedException {
            int iterations = 50;

            Runnable recorder = () -> {
                for (int i = 0; i < iterations; i++) {
                    testSkill.recordSuccess(true);
                    testSkill.getExecutionCount();
                    testSkill.getSuccessRate();
                }
            };

            Thread t1 = new Thread(recorder);
            Thread t2 = new Thread(recorder);
            Thread t3 = new Thread(recorder);

            t1.start();
            t2.start();
            t3.start();

            t1.join();
            t2.join();
            t3.join();

            assertEquals(iterations * 3, testSkill.getExecutionCount(),
                "Execution count should be accurate");
        }
    }
}
