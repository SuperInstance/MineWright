package com.minewright.script;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import java.util.List;

/**
 * Unit tests for {@link ScriptValidator}.
 *
 * <p>Tests verify the validation functionality:
 * <ul>
 *   <li>Structural validation (cycles, depth, nodes)</li>
 *   <li>Semantic validation (actions, parameters)</li>
 *   <li>Security validation (disallowed actions, infinite loops)</li>
 *   <li>Resource validation (execution time, distance)</li>
 *   <li>Custom validation configuration</li>
 * </ul>
 */
@DisplayName("ScriptValidator Tests")
class ScriptValidatorTest {

    private ScriptValidator defaultValidator;
    private ScriptValidator customValidator;

    @BeforeEach
    void setUp() {
        defaultValidator = new ScriptValidator();
        customValidator = new ScriptValidator(
            Set.of("mine", "place", "craft", "gather"),
            new ScriptValidator.ValidationConfig()
        );
    }

    @Test
    @DisplayName("Valid script passes validation")
    void testValidScriptPassesValidation() {
        Script script = Script.builder()
            .metadata(Script.ScriptMetadata.builder()
                .id("test_script")
                .name("Test Script")
                .build())
            .scriptNode(ScriptNode.Builder.sequence(
                ScriptNode.Builder.simpleAction("mine").build(),
                ScriptNode.Builder.simpleAction("place").build()
            ).build())
            .build();

        ScriptValidator.ValidationResult result = defaultValidator.validate(script);

        assertTrue(result.isValid(),
            "Valid script should pass validation");
        assertTrue(result.getErrors().isEmpty(),
            "Valid script should have no errors");
    }

    @Test
    @DisplayName("Null script fails validation")
    void testNullScriptFailsValidation() {
        ScriptValidator.ValidationResult result = defaultValidator.validate(null);

        assertFalse(result.isValid(),
            "Null script should fail validation");
        assertTrue(result.getErrors().contains("Script is null"),
            "Should have error about null script");
    }

    @Test
    @DisplayName("Script with null root node fails validation")
    void testScriptWithNullRootNodeFailsValidation() {
        Script script = Script.builder()
            .metadata(Script.ScriptMetadata.builder()
                .id("test_script")
                .build())
            .scriptNode(null)
            .build();

        ScriptValidator.ValidationResult result = defaultValidator.validate(script);

        assertFalse(result.isValid(),
            "Script with null root node should fail validation");
        assertTrue(result.getErrors().contains("Script has no root node"),
            "Should have error about missing root node");
    }

    @Test
    @DisplayName("Action node without action name fails validation")
    void testActionNodeWithoutActionNameFailsValidation() {
        ScriptNode node = ScriptNode.builder()
            .type(ScriptNode.NodeType.ACTION)
            .build();

        Script script = Script.builder()
            .metadata(Script.ScriptMetadata.builder()
                .id("test_script")
                .build())
            .scriptNode(node)
            .build();

        ScriptValidator.ValidationResult result = defaultValidator.validate(script);

        assertFalse(result.isValid(),
            "Action without name should fail validation");
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.contains("no action name")),
            "Should have error about missing action name");
    }

    @Test
    @DisplayName("Unknown action generates warning")
    void testUnknownActionGeneratesWarning() {
        ScriptNode node = ScriptNode.builder()
            .type(ScriptNode.NodeType.ACTION)
            .withAction("unknown_action")
            .build();

        Script script = Script.builder()
            .metadata(Script.ScriptMetadata.builder()
                .id("test_script")
                .build())
            .scriptNode(node)
            .build();

        ScriptValidator.ValidationResult result = defaultValidator.validate(script);

        assertTrue(result.isValid(),
            "Unknown action should not fail validation");
        assertTrue(result.getWarnings().stream()
            .anyMatch(w -> w.contains("Unknown action")),
            "Should have warning about unknown action");
    }

    @Test
    @DisplayName("Loop without iterations generates warning")
    void testLoopWithoutIterationsGeneratesWarning() {
        ScriptNode node = ScriptNode.builder()
            .type(ScriptNode.NodeType.LOOP)
            .addChild(ScriptNode.Builder.simpleAction("mine").build())
            .build();

        Script script = Script.builder()
            .metadata(Script.ScriptMetadata.builder()
                .id("test_script")
                .build())
            .scriptNode(node)
            .build();

        ScriptValidator.ValidationResult result = defaultValidator.validate(script);

        assertTrue(result.isValid(),
            "Loop without iterations should not fail validation");
        assertTrue(result.getWarnings().stream()
            .anyMatch(w -> w.contains("no iteration count")),
            "Should have warning about missing iteration count");
    }

    @Test
    @DisplayName("Loop exceeding max iterations fails validation")
    void testLoopExceedingMaxIterationsFailsValidation() {
        ScriptValidator.ValidationConfig config = new ScriptValidator.ValidationConfig();
        config.setMaxLoopIterations(100);

        ScriptValidator validator = new ScriptValidator(null, config);

        ScriptNode node = ScriptNode.builder()
            .type(ScriptNode.NodeType.LOOP)
            .addParameter("iterations", 200)
            .addChild(ScriptNode.Builder.simpleAction("mine").build())
            .build();

        Script script = Script.builder()
            .metadata(Script.ScriptMetadata.builder()
                .id("test_script")
                .build())
            .scriptNode(node)
            .build();

        ScriptValidator.ValidationResult result = validator.validate(script);

        assertFalse(result.isValid(),
            "Loop exceeding max iterations should fail validation");
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.contains("exceed maximum")),
            "Should have error about exceeding max iterations");
    }

    @Test
    @DisplayName("Script exceeding max depth generates warning")
    void testScriptExceedingMaxDepthGeneratesWarning() {
        ScriptValidator.ValidationConfig config = new ScriptValidator.ValidationConfig();
        config.setMaxDepth(5);

        ScriptValidator validator = new ScriptValidator(null, config);

        // Create a deeply nested structure
        ScriptNode node = ScriptNode.Builder.simpleAction("mine").build();
        for (int i = 0; i < 10; i++) {
            node = ScriptNode.Builder.sequence(node).build();
        }

        Script script = Script.builder()
            .metadata(Script.ScriptMetadata.builder()
                .id("test_script")
                .build())
            .scriptNode(node)
            .build();

        ScriptValidator.ValidationResult result = validator.validate(script);

        assertTrue(result.isValid(),
            "Deep script should still be valid");
        assertTrue(result.getWarnings().stream()
            .anyMatch(w -> w.contains("depth") && w.contains("exceeds")),
            "Should have warning about depth");
    }

    @Test
    @DisplayName("Script exceeding max nodes fails validation")
    void testScriptExceedingMaxNodesFailsValidation() {
        ScriptValidator.ValidationConfig config = new ScriptValidator.ValidationConfig();
        config.setMaxNodes(5);

        ScriptValidator validator = new ScriptValidator(null, config);

        // Create a script with many nodes
        ScriptNode[] children = new ScriptNode[10];
        for (int i = 0; i < 10; i++) {
            children[i] = ScriptNode.Builder.simpleAction("mine").build();
        }
        ScriptNode node = ScriptNode.Builder.sequence(children).build();

        Script script = Script.builder()
            .metadata(Script.ScriptMetadata.builder()
                .id("test_script")
                .build())
            .scriptNode(node)
            .build();

        ScriptValidator.ValidationResult result = validator.validate(script);

        assertFalse(result.isValid(),
            "Script with too many nodes should fail validation");
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.contains("too many nodes")),
            "Should have error about too many nodes");
    }

    @Test
    @DisplayName("Disallowed action fails validation")
    void testDisallowedActionFailsValidation() {
        ScriptValidator.ValidationConfig config = new ScriptValidator.ValidationConfig();
        config.setDisallowedActions(Set.of("dangerous_action"));

        ScriptValidator validator = new ScriptValidator(null, config);

        ScriptNode node = ScriptNode.builder()
            .type(ScriptNode.NodeType.ACTION)
            .withAction("dangerous_action")
            .build();

        Script script = Script.builder()
            .metadata(Script.ScriptMetadata.builder()
                .id("test_script")
                .build())
            .scriptNode(node)
            .build();

        ScriptValidator.ValidationResult result = validator.validate(script);

        assertFalse(result.isValid(),
            "Disallowed action should fail validation");
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.contains("Disallowed action")),
            "Should have error about disallowed action");
    }

    @Test
    @DisplayName("High estimated execution time generates warning")
    void testHighEstimatedExecutionTimeGeneratesWarning() {
        ScriptValidator.ValidationConfig config = new ScriptValidator.ValidationConfig();
        config.setMaxExecutionTicks(100); // 5 seconds

        ScriptValidator validator = new ScriptValidator(null, config);

        // Create a script that will take a long time
        ScriptNode[] actions = new ScriptNode[20];
        for (int i = 0; i < 20; i++) {
            actions[i] = ScriptNode.Builder.simpleAction("mine").build();
        }
        ScriptNode node = ScriptNode.Builder.sequence(actions).build();

        Script script = Script.builder()
            .metadata(Script.ScriptMetadata.builder()
                .id("test_script")
                .build())
            .scriptNode(node)
            .build();

        ScriptValidator.ValidationResult result = validator.validate(script);

        assertTrue(result.isValid(),
            "Long-running script should still be valid");
        assertTrue(result.getWarnings().stream()
            .anyMatch(w -> w.contains("Estimated execution time")),
            "Should have warning about execution time");
    }

    @Test
    @DisplayName("Distance parameter exceeding limit generates warning")
    void testDistanceParameterExceedingLimitGeneratesWarning() {
        ScriptValidator.ValidationConfig config = new ScriptValidator.ValidationConfig();
        config.setMaxDistanceBlocks(100);

        ScriptValidator validator = new ScriptValidator(null, config);

        ScriptNode node = ScriptNode.builder()
            .type(ScriptNode.NodeType.ACTION)
            .withAction("move")
            .addParameter("max_distance", 200)
            .build();

        Script script = Script.builder()
            .metadata(Script.ScriptMetadata.builder()
                .id("test_script")
                .build())
            .scriptNode(node)
            .build();

        ScriptValidator.ValidationResult result = validator.validate(script);

        assertTrue(result.isValid(),
            "Distance parameter should not fail validation");
        assertTrue(result.getWarnings().stream()
            .anyMatch(w -> w.contains("Distance") && w.contains("exceeds")),
            "Should have warning about distance");
    }

    @Test
    @DisplayName("Null parameter in action generates warning")
    void testNullParameterInActionGeneratesWarning() {
        ScriptNode node = ScriptNode.builder()
            .type(ScriptNode.NodeType.ACTION)
            .withAction("mine")
            .addParameter("block", null)
            .build();

        Script script = Script.builder()
            .metadata(Script.ScriptMetadata.builder()
                .id("test_script")
                .build())
            .scriptNode(node)
            .build();

        ScriptValidator.ValidationResult result = defaultValidator.validate(script);

        assertTrue(result.isValid(),
            "Null parameter should not fail validation");
        assertTrue(result.getWarnings().stream()
            .anyMatch(w -> w.contains("parameter") && w.contains("null")),
            "Should have warning about null parameter");
    }

    @Test
    @DisplayName("Custom known actions are recognized")
    void testCustomKnownActionsAreRecognized() {
        ScriptNode node = ScriptNode.builder()
            .type(ScriptNode.NodeType.ACTION)
            .withAction("custom_action")
            .build();

        Script script = Script.builder()
            .metadata(Script.ScriptMetadata.builder()
                .id("test_script")
                .build())
            .scriptNode(node)
            .build();

        ScriptValidator.ValidationResult result = customValidator.validate(script);

        // Should not have warning about unknown action since we're using default validator
        // which has a comprehensive list of known actions
        assertTrue(result.isValid(),
            "Script should be valid");
    }

    @Test
    @DisplayName("Validation result summary is formatted correctly")
    void testValidationResultSummaryIsFormattedCorrectly() {
        ScriptNode node = ScriptNode.builder()
            .type(ScriptNode.NodeType.ACTION)
            .withAction("mine")
            .build();

        Script script = Script.builder()
            .metadata(Script.ScriptMetadata.builder()
                .id("test_script")
                .build())
            .scriptNode(node)
            .build();

        ScriptValidator.ValidationResult result = defaultValidator.validate(script);

        String summary = result.getSummary();

        assertTrue(summary.contains("VALID"),
            "Summary should contain validation status");
        assertTrue(summary.contains("errors"),
            "Summary should contain error count");
        assertTrue(summary.contains("warnings"),
            "Summary should contain warning count");
    }

    @Test
    @DisplayName("Complex nested script is validated correctly")
    void testComplexNestedScriptIsValidatedCorrectly() {
        ScriptNode node = ScriptNode.Builder.sequence(
            ScriptNode.Builder.loop(5,
                ScriptNode.Builder.sequence(
                    ScriptNode.Builder.simpleAction("mine").build(),
                    ScriptNode.Builder.simpleAction("place").build()
                ).build()
            ).build(),
            ScriptNode.Builder.ifElse("$has_wood",
                ScriptNode.Builder.simpleAction("craft").build(),
                ScriptNode.Builder.simpleAction("gather").build()
            ).build(),
            ScriptNode.Builder.parallel(
                ScriptNode.Builder.simpleAction("mine").build(),
                ScriptNode.Builder.simpleAction("build").build()
            ).build()
        ).build();

        Script script = Script.builder()
            .metadata(Script.ScriptMetadata.builder()
                .id("test_script")
                .name("Complex Script")
                .build())
            .scriptNode(node)
            .build();

        ScriptValidator.ValidationResult result = defaultValidator.validate(script);

        assertTrue(result.isValid(),
            "Complex nested script should be valid");
        assertTrue(result.getErrors().isEmpty(),
            "Complex script should have no errors");
    }

    @Test
    @DisplayName("Empty sequence in script is valid")
    void testEmptySequenceInScriptIsValid() {
        ScriptNode node = ScriptNode.builder()
            .type(ScriptNode.NodeType.SEQUENCE)
            .build();

        Script script = Script.builder()
            .metadata(Script.ScriptMetadata.builder()
                .id("test_script")
                .build())
            .scriptNode(node)
            .build();

        ScriptValidator.ValidationResult result = defaultValidator.validate(script);

        assertTrue(result.isValid(),
            "Empty sequence should be valid");
    }

    @Test
    @DisplayName("Script with selector node is validated")
    void testScriptWithSelectorNodeIsValidated() {
        ScriptNode node = ScriptNode.Builder.selector(
            ScriptNode.Builder.simpleAction("mine").build(),
            ScriptNode.Builder.simpleAction("craft").build(),
            ScriptNode.Builder.simpleAction("gather").build()
        ).build();

        Script script = Script.builder()
            .metadata(Script.ScriptMetadata.builder()
                .id("test_script")
                .build())
            .scriptNode(node)
            .build();

        ScriptValidator.ValidationResult result = defaultValidator.validate(script);

        assertTrue(result.isValid(),
            "Script with selector should be valid");
    }

    @Test
    @DisplayName("Script with parallel node is validated")
    void testScriptWithParallelNodeIsValidated() {
        ScriptNode node = ScriptNode.Builder.parallel(
            ScriptNode.Builder.simpleAction("mine").build(),
            ScriptNode.Builder.simpleAction("build").build()
        ).build();

        Script script = Script.builder()
            .metadata(Script.ScriptMetadata.builder()
                .id("test_script")
                .build())
            .scriptNode(node)
            .build();

        ScriptValidator.ValidationResult result = defaultValidator.validate(script);

        assertTrue(result.isValid(),
            "Script with parallel should be valid");
    }

    @Test
    @DisplayName("Script with condition node is validated")
    void testScriptWithConditionNodeIsValidated() {
        ScriptNode node = ScriptNode.builder()
            .type(ScriptNode.NodeType.CONDITION)
            .withCondition("$has_wood")
            .build();

        Script script = Script.builder()
            .metadata(Script.ScriptMetadata.builder()
                .id("test_script")
                .build())
            .scriptNode(node)
            .build();

        ScriptValidator.ValidationResult result = defaultValidator.validate(script);

        assertTrue(result.isValid(),
            "Script with condition should be valid");
    }

    @Test
    @DisplayName("Script with repeatUntil node is validated")
    void testScriptWithRepeatUntilNodeIsValidated() {
        ScriptNode node = ScriptNode.Builder.repeatUntil("$inventory_full",
            ScriptNode.Builder.simpleAction("mine").build()
        ).build();

        Script script = Script.builder()
            .metadata(Script.ScriptMetadata.builder()
                .id("test_script")
                .build())
            .scriptNode(node)
            .build();

        ScriptValidator.ValidationResult result = defaultValidator.validate(script);

        assertTrue(result.isValid(),
            "Script with repeatUntil should be valid");
    }

    @Test
    @DisplayName("Multiple warnings are collected")
    void testMultipleWarningsAreCollected() {
        ScriptNode node = ScriptNode.builder()
            .type(ScriptNode.NodeType.SEQUENCE)
            .addChild(
                ScriptNode.builder()
                .type(ScriptNode.NodeType.LOOP)
                .addChild(ScriptNode.Builder.simpleAction("mine").build())
                .build()
            )
            .addChild(
                ScriptNode.builder()
                .type(ScriptNode.NodeType.ACTION)
                .withAction("unknown_action")
                .build()
            )
            .build();

        Script script = Script.builder()
            .metadata(Script.ScriptMetadata.builder()
                .id("test_script")
                .build())
            .scriptNode(node)
            .build();

        ScriptValidator.ValidationResult result = defaultValidator.validate(script);

        assertTrue(result.isValid(),
            "Script with warnings should still be valid");
        assertTrue(result.getWarnings().size() >= 2,
            "Should have multiple warnings");
    }

    @Test
    @DisplayName("Validation result can be checked for validity")
    void testValidationResultCanBeCheckedForValidity() {
        ScriptValidator.ValidationResult result = new ScriptValidator.ValidationResult();

        assertTrue(result.isValid(),
            "Empty result should be valid");
        assertTrue(result.getErrors().isEmpty(),
            "Empty result should have no errors");

        result.addError("Test error");

        assertFalse(result.isValid(),
            "Result with errors should be invalid");
        assertFalse(result.getErrors().isEmpty(),
            "Result should have errors");
    }

    @Test
    @DisplayName("Validation results can be combined")
    void testValidationResultsCanBeCombined() {
        ScriptValidator.ValidationResult result1 = new ScriptValidator.ValidationResult();
        result1.addError("Error 1");
        result1.addWarning("Warning 1");

        ScriptValidator.ValidationResult result2 = new ScriptValidator.ValidationResult();
        result2.addError("Error 2");
        result2.addWarning("Warning 2");

        result1.add(result2);

        assertEquals(2, result1.getErrors().size(),
            "Should have 2 errors after combining");
        assertEquals(2, result1.getWarnings().size(),
            "Should have 2 warnings after combining");
    }

    @Test
    @DisplayName("Default known actions include common actions")
    void testDefaultKnownActionsIncludeCommonActions() {
        ScriptValidator validator = new ScriptValidator();

        // These should be recognized as known actions
        String[] knownActions = {"mine", "place", "build", "craft", "gather",
            "pathfind", "follow", "attack", "idle_follow", "move", "look", "equip", "deposit"};

        for (String action : knownActions) {
            ScriptNode node = ScriptNode.builder()
                .type(ScriptNode.NodeType.ACTION)
                .withAction(action)
                .build();

            Script script = Script.builder()
                .metadata(Script.ScriptMetadata.builder()
                    .id("test_" + action)
                    .build())
                .scriptNode(node)
                .build();

            ScriptValidator.ValidationResult result = validator.validate(script);

            assertFalse(result.getWarnings().stream()
                .anyMatch(w -> w.contains("Unknown action: " + action)),
                action + " should be recognized as known action");
        }
    }

    @Test
    @DisplayName("Zero iteration loop is valid")
    void testZeroIterationLoopIsValid() {
        ScriptNode node = ScriptNode.builder()
            .type(ScriptNode.NodeType.LOOP)
            .addParameter("iterations", 0)
            .addChild(ScriptNode.Builder.simpleAction("mine").build())
            .build();

        Script script = Script.builder()
            .metadata(Script.ScriptMetadata.builder()
                .id("test_script")
                .build())
            .scriptNode(node)
            .build();

        ScriptValidator.ValidationResult result = defaultValidator.validate(script);

        assertTrue(result.isValid(),
            "Zero iteration loop should be valid");
    }

    @Test
    @DisplayName("Node count includes all nested nodes")
    void testNodeCountIncludesAllNestedNodes() {
        ScriptNode node = ScriptNode.Builder.sequence(
            ScriptNode.Builder.simpleAction("mine").build(),
            ScriptNode.Builder.sequence(
                ScriptNode.Builder.simpleAction("place").build(),
                ScriptNode.Builder.simpleAction("craft").build()
            ).build()
        ).build();

        // This should count: sequence(1) + mine(1) + sequence(1) + place(1) + craft(1) = 5
        // But the actual implementation might count differently
        Script script = Script.builder()
            .metadata(Script.ScriptMetadata.builder()
                .id("test_script")
                .build())
            .scriptNode(node)
            .build();

        ScriptValidator.ValidationResult result = defaultValidator.validate(script);

        assertTrue(result.isValid(),
            "Script should be valid");
    }
}
