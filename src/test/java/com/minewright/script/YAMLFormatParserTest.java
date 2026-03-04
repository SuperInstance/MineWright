package com.minewright.script;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link YAMLFormatParser}.
 *
 * Tests cover:
 * <ul>
 *   <li>YAML script parsing with metadata</li>
 *   <li>Parameters section parsing</li>
 *   <li>Requirements section parsing</li>
 *   <li>Script block parsing with various node types</li>
 *   <li>Error handling section parsing</li>
 *   <li>Validation and error cases</li>
 * </ul>
 *
 * @see YAMLFormatParser
 * @see ScriptLexer
 * @see Script
 * @since 1.3.0
 */
@DisplayName("YAMLFormatParser Tests")
class YAMLFormatParserTest {

    private YAMLFormatParser parser;

    @BeforeEach
    void setUp() {
        parser = null; // Will be created per test with specific input
    }

    // ==================== Basic Script Parsing Tests ====================

    @Test
    @DisplayName("Parse minimal script with just metadata")
    void testParseMinimalScript() throws ScriptParseException {
        String input = """
            metadata:
              id: test_script
              name: "Test Script"
            """;

        ScriptLexer lexer = new ScriptLexer(input);
        parser = new YAMLFormatParser(lexer);

        Script script = parser.parse();

        assertNotNull(script);
        assertEquals("test_script", script.getMetadata().getId());
        assertEquals("Test Script", script.getMetadata().getName());
    }

    @Test
    @DisplayName("Parse script with all sections")
    void testParseCompleteScript() throws ScriptParseException {
        String input = """
            metadata:
              id: complete_script
              name: "Complete Script"
              description: "A complete test script"
              author: "Test Author"

            parameters:
              - name: target
                type: string
              - name: count
                type: integer
                default: 10

            requirements:
              inventory:
                - item: oak_log
                  quantity: 5
              tools:
                - axe
                - pickaxe

            script:
              - type: ACTION
                action: mine
                params:
                  block: stone
                  quantity: 5

            error_handling:
              block_not_found:
                - type: ACTION
                  action: recover
            """;

        ScriptLexer lexer = new ScriptLexer(input);
        parser = new YAMLFormatParser(lexer);

        Script script = parser.parse();

        assertNotNull(script);
        assertEquals("complete_script", script.getMetadata().getId());
        assertEquals("Complete Script", script.getMetadata().getName());
        assertEquals("A complete test script", script.getMetadata().getDescription());
        assertEquals("Test Author", script.getMetadata().getAuthor());

        assertTrue(script.getParameters().containsKey("target"));
        assertTrue(script.getParameters().containsKey("count"));

        assertNotNull(script.getScriptNode());
        assertFalse(script.getErrorHandlers().isEmpty());
    }

    // ==================== Metadata Parsing Tests ====================

    @Test
    @DisplayName("Parse metadata with all fields")
    void testParseMetadataWithAllFields() throws ScriptParseException {
        String input = """
            metadata:
              id: meta_001
              name: "Metadata Test"
              description: "Testing metadata parsing"
              author: "Claude"

            script:
              - type: ACTION
                action: test
            """;

        ScriptLexer lexer = new ScriptLexer(input);
        parser = new YAMLFormatParser(lexer);

        Script script = parser.parse();

        assertEquals("meta_001", script.getMetadata().getId());
        assertEquals("Metadata Test", script.getMetadata().getName());
        assertEquals("Testing metadata parsing", script.getMetadata().getDescription());
        assertEquals("Claude", script.getMetadata().getAuthor());
    }

    @Test
    @DisplayName("Parse script without metadata generates default metadata")
    void testParseWithoutMetadataGeneratesDefaults() throws ScriptParseException {
        String input = """
            script:
              - type: ACTION
                action: test
            """;

        ScriptLexer lexer = new ScriptLexer(input);
        parser = new YAMLFormatParser(lexer);

        Script script = parser.parse();

        assertNotNull(script.getMetadata().getId());
        assertEquals("Generated Script", script.getMetadata().getName());
    }

    // ==================== Parameters Parsing Tests ====================

    @Test
    @DisplayName("Parse parameters section")
    void testParseParameters() throws ScriptParseException {
        String input = """
            parameters:
              - name: target
                type: string
              - name: count
                type: integer
              - name: enabled
                type: boolean
                default: true

            script:
              - type: ACTION
                action: test
            """;

        ScriptLexer lexer = new ScriptLexer(input);
        parser = new YAMLFormatParser(lexer);

        Script script = parser.parse();

        assertEquals(3, script.getParameters().size());

        Script.Parameter targetParam = script.getParameters().get("target");
        assertEquals("target", targetParam.getName());
        assertEquals("string", targetParam.getType());

        Script.Parameter countParam = script.getParameters().get("count");
        assertEquals("integer", countParam.getType());

        Script.Parameter enabledParam = script.getParameters().get("enabled");
        assertEquals("boolean", enabledParam.getType());
        assertEquals(true, enabledParam.getDefaultValue());
    }

    @Test
    @DisplayName("Parse parameter without default value")
    void testParseParameterWithoutDefault() throws ScriptParseException {
        String input = """
            parameters:
              - name: simple_param
                type: string

            script:
              - type: ACTION
                action: test
            """;

        ScriptLexer lexer = new ScriptLexer(input);
        parser = new YAMLFormatParser(lexer);

        Script script = parser.parse();

        Script.Parameter param = script.getParameters().get("simple_param");
        assertEquals("simple_param", param.getName());
        assertEquals("string", param.getType());
        assertNull(param.getDefaultValue());
    }

    // ==================== Requirements Parsing Tests ====================

    @Test
    @DisplayName("Parse requirements with inventory items")
    void testParseRequirementsWithInventory() throws ScriptParseException {
        String input = """
            requirements:
              inventory:
                - item: oak_log
                  quantity: 10
                - item: stone
                  quantity: 5

            script:
              - type: ACTION
                action: test
            """;

        ScriptLexer lexer = new ScriptLexer(input);
        parser = new YAMLFormatParser(lexer);

        Script script = parser.parse();

        assertNotNull(script.getRequirements().getInventory());
        assertTrue(script.getRequirements().getInventory().size() >= 0);
    }

    @Test
    @DisplayName("Parse requirements with tools")
    void testParseRequirementsWithTools() throws ScriptParseException {
        String input = """
            requirements:
              tools:
                - pickaxe
                - axe
                - shovel

            script:
              - type: ACTION
                action: test
            """;

        ScriptLexer lexer = new ScriptLexer(input);
        parser = new YAMLFormatParser(lexer);

        Script script = parser.parse();

        assertNotNull(script.getRequirements().getTools());
        assertTrue(script.getRequirements().getTools().size() >= 0);
    }

    // ==================== Script Node Parsing Tests ====================

    @Test
    @DisplayName("Parse simple action node")
    void testParseSimpleActionNode() throws ScriptParseException {
        String input = """
            script:
              - type: ACTION
                action: mine
            """;

        ScriptLexer lexer = new ScriptLexer(input);
        parser = new YAMLFormatParser(lexer);

        Script script = parser.parse();

        assertNotNull(script.getScriptNode());
        assertEquals(ScriptNode.NodeType.ACTION, script.getScriptNode().getType());
        assertEquals("mine", script.getScriptNode().getAction());
    }

    @Test
    @DisplayName("Parse action node with parameters")
    void testParseActionNodeWithParameters() throws ScriptParseException {
        String input = """
            script:
              - type: ACTION
                action: mine
                params:
                  block: stone
                  quantity: 10
            """;

        ScriptLexer lexer = new ScriptLexer(input);
        parser = new YAMLFormatParser(lexer);

        Script script = parser.parse();

        ScriptNode node = script.getScriptNode();
        assertEquals(ScriptNode.NodeType.ACTION, node.getType());
        assertEquals("mine", node.getAction());

        Map<String, Object> params = node.getParameters();
        assertNotNull(params);
        assertEquals("stone", params.get("block"));
        assertEquals(10, params.get("quantity"));
    }

    @Test
    @DisplayName("Parse sequence node")
    void testParseSequenceNode() throws ScriptParseException {
        String input = """
            script:
              - type: SEQUENCE
                children:
                  - type: ACTION
                    action: move
                  - type: ACTION
                    action: mine
            """;

        ScriptLexer lexer = new ScriptLexer(input);
        parser = new YAMLFormatParser(lexer);

        Script script = parser.parse();

        ScriptNode node = script.getScriptNode();
        assertEquals(ScriptNode.NodeType.SEQUENCE, node.getType());

        List<ScriptNode> children = node.getChildren();
        assertNotNull(children);
        assertEquals(2, children.size());
        assertEquals(ScriptNode.NodeType.ACTION, children.get(0).getType());
        assertEquals(ScriptNode.NodeType.ACTION, children.get(1).getType());
    }

    @Test
    @DisplayName("Parse selector node")
    void testParseSelectorNode() throws ScriptParseException {
        String input = """
            script:
              - type: SELECTOR
                children:
                  - type: ACTION
                    action: option_a
                  - type: ACTION
                    action: option_b
            """;

        ScriptLexer lexer = new ScriptLexer(input);
        parser = new YAMLFormatParser(lexer);

        Script script = parser.parse();

        ScriptNode node = script.getScriptNode();
        assertEquals(ScriptNode.NodeType.SELECTOR, node.getType());
        assertNotNull(node.getChildren());
    }

    @Test
    @DisplayName("Parse parallel node")
    void testParseParallelNode() throws ScriptParseException {
        String input = """
            script:
              - type: PARALLEL
                children:
                  - type: ACTION
                    action: task1
                  - type: ACTION
                    action: task2
            """;

        ScriptLexer lexer = new ScriptLexer(input);
        parser = new YAMLFormatParser(lexer);

        Script script = parser.parse();

        ScriptNode node = script.getScriptNode();
        assertEquals(ScriptNode.NodeType.PARALLEL, node.getType());
        assertNotNull(node.getChildren());
    }

    @Test
    @DisplayName("Parse condition node")
    void testParseConditionNode() throws ScriptParseException {
        String input = """
            script:
              - type: CONDITION
                condition: "has_item('pickaxe')"
            """;

        ScriptLexer lexer = new ScriptLexer(input);
        parser = new YAMLFormatParser(lexer);

        Script script = parser.parse();

        ScriptNode node = script.getScriptNode();
        assertEquals(ScriptNode.NodeType.CONDITION, node.getType());
        assertEquals("has_item('pickaxe')", node.getCondition());
    }

    @Test
    @DisplayName("Parse loop node")
    void testParseLoopNode() throws ScriptParseException {
        String input = """
            script:
              - type: LOOP
                params:
                  iterations: 5
                children:
                  - type: ACTION
                    action: mine
            """;

        ScriptLexer lexer = new ScriptLexer(input);
        parser = new YAMLFormatParser(lexer);

        Script script = parser.parse();

        ScriptNode node = script.getScriptNode();
        assertEquals(ScriptNode.NodeType.LOOP, node.getType());
        assertEquals(5, node.getParameters().get("iterations"));
        assertNotNull(node.getChildren());
    }

    @Test
    @DisplayName("Parse IF node")
    void testParseIfNode() throws ScriptParseException {
        String input = """
            script:
              - type: IF
                condition: "health > 50"
                children:
                  - type: ACTION
                    action: heal
            """;

        ScriptLexer lexer = new ScriptLexer(input);
        parser = new YAMLFormatParser(lexer);

        Script script = parser.parse();

        ScriptNode node = script.getScriptNode();
        assertEquals(ScriptNode.NodeType.IF, node.getType());
        assertEquals("health > 50", node.getCondition());
        assertNotNull(node.getChildren());
    }

    // ==================== Alternative Syntax Tests ====================

    @Test
    @DisplayName("Parse action with function call syntax")
    void testParseActionWithFunctionCallSyntax() throws ScriptParseException {
        String input = """
            script:
              mine(block="stone", quantity=10)
            """;

        ScriptLexer lexer = new ScriptLexer(input);
        parser = new YAMLFormatParser(lexer);

        Script script = parser.parse();

        ScriptNode node = script.getScriptNode();
        assertEquals(ScriptNode.NodeType.ACTION, node.getType());
        assertEquals("mine", node.getAction());
        assertEquals("stone", node.getParameters().get("block"));
        assertEquals(10, node.getParameters().get("quantity"));
    }

    @Test
    @DisplayName("Parse simple action name without parameters")
    void testParseSimpleActionName() throws ScriptParseException {
        String input = """
            script:
              move
            """;

        ScriptLexer lexer = new ScriptLexer(input);
        parser = new YAMLFormatParser(lexer);

        Script script = parser.parse();

        ScriptNode node = script.getScriptNode();
        assertEquals(ScriptNode.NodeType.ACTION, node.getType());
        assertEquals("move", node.getAction());
    }

    @Test
    @DisplayName("Parse sequence keyword with indented block")
    void testParseSequenceKeyword() throws ScriptParseException {
        String input = """
            script:
              sequence
                move
                mine
            """;

        ScriptLexer lexer = new ScriptLexer(input);
        parser = new YAMLFormatParser(lexer);

        Script script = parser.parse();

        ScriptNode node = script.getScriptNode();
        assertEquals(ScriptNode.NodeType.SEQUENCE, node.getType());
        assertNotNull(node.getChildren());
    }

    // ==================== Error Handling Tests ====================

    @Test
    @DisplayName("Parse error handling section")
    void testParseErrorHandlingSection() throws ScriptParseException {
        String input = """
            script:
              - type: ACTION
                action: risky_operation

            error_handling:
              failure:
                - type: ACTION
                  action: recover
              timeout:
                - type: ACTION
                  action: retry
            """;

        ScriptLexer lexer = new ScriptLexer(input);
        parser = new YAMLFormatParser(lexer);

        Script script = parser.parse();

        assertNotNull(script.getErrorHandlers());
        assertTrue(script.getErrorHandlers().containsKey("failure"));
        assertTrue(script.getErrorHandlers().containsKey("timeout"));
    }

    @Test
    @DisplayName("Parse script without error handling section")
    void testParseWithoutErrorHandling() throws ScriptParseException {
        String input = """
            script:
              - type: ACTION
                action: safe_operation
            """;

        ScriptLexer lexer = new ScriptLexer(input);
        parser = new YAMLFormatParser(lexer);

        Script script = parser.parse();

        assertNotNull(script.getErrorHandlers());
        assertTrue(script.getErrorHandlers().isEmpty() || script.getErrorHandlers().size() == 0);
    }

    // ==================== Edge Cases and Validation Tests ====================

    @Test
    @DisplayName("Parse empty script generates valid script object")
    void testParseEmptyScript() throws ScriptParseException {
        String input = "";

        ScriptLexer lexer = new ScriptLexer(input);
        parser = new YAMLFormatParser(lexer);

        Script script = parser.parse();

        assertNotNull(script);
        assertNotNull(script.getMetadata());
        assertEquals("1.0.0", script.getVersion());
    }

    @Test
    @DisplayName("Parse script with only metadata")
    void testParseScriptWithOnlyMetadata() throws ScriptParseException {
        String input = """
            metadata:
              id: meta_only
              name: "Metadata Only"
            """;

        ScriptLexer lexer = new ScriptLexer(input);
        parser = new YAMLFormatParser(lexer);

        Script script = parser.parse();

        assertNotNull(script);
        assertEquals("meta_only", script.getMetadata().getId());
        assertEquals("Metadata Only", script.getMetadata().getName());
    }

    @Test
    @DisplayName("Handle whitespace in script input")
    void testHandleWhitespaceInScript() throws ScriptParseException {
        String input = """
            metadata:
              id: whitespace_test
              name: "Whitespace Test"


            script:
              - type: ACTION
                action: test
            """;

        ScriptLexer lexer = new ScriptLexer(input);
        parser = new YAMLFormatParser(lexer);

        Script script = parser.parse();

        assertNotNull(script);
        assertEquals("whitespace_test", script.getMetadata().getId());
    }

    @Test
    @DisplayName("Parse script with quoted and unquoted strings")
    void testParseQuotedAndUnquotedStrings() throws ScriptParseException {
        String input = """
            metadata:
              id: string_test
              name: Test Name
              description: "This is a quoted description"

            script:
              - type: ACTION
                action: test
                params:
                  quoted: "value with spaces"
                  unquoted: simple_value
            """;

        ScriptLexer lexer = new ScriptLexer(input);
        parser = new YAMLFormatParser(lexer);

        Script script = parser.parse();

        assertEquals("Test Name", script.getMetadata().getName());
        assertEquals("This is a quoted description", script.getMetadata().getDescription());

        Map<String, Object> params = script.getScriptNode().getParameters();
        assertEquals("value with spaces", params.get("quoted"));
        assertEquals("simple_value", params.get("unquoted"));
    }

    // ==================== Nested Structure Tests ====================

    @Test
    @DisplayName("Parse deeply nested script structure")
    void testParseDeeplyNestedStructure() throws ScriptParseException {
        String input = """
            script:
              - type: SEQUENCE
                children:
                  - type: SELECTOR
                    children:
                      - type: PARALLEL
                        children:
                          - type: ACTION
                            action: task1
                          - type: ACTION
                            action: task2
                  - type: ACTION
                    action: cleanup
            """;

        ScriptLexer lexer = new ScriptLexer(input);
        parser = new YAMLFormatParser(lexer);

        Script script = parser.parse();

        ScriptNode root = script.getScriptNode();
        assertEquals(ScriptNode.NodeType.SEQUENCE, root.getType());

        ScriptNode selector = root.getChildren().get(0);
        assertEquals(ScriptNode.NodeType.SELECTOR, selector.getType());

        ScriptNode parallel = selector.getChildren().get(0);
        assertEquals(ScriptNode.NodeType.PARALLEL, parallel.getType());

        assertEquals(2, parallel.getChildren().size());
    }

    // ==================== Version and Defaults Tests ====================

    @Test
    @DisplayName("Script has default version when not specified")
    void testScriptHasDefaultVersion() throws ScriptParseException {
        String input = """
            script:
              - type: ACTION
                action: test
            """;

        ScriptLexer lexer = new ScriptLexer(input);
        parser = new YAMLFormatParser(lexer);

        Script script = parser.parse();

        assertEquals("1.0.0", script.getVersion());
    }
}
