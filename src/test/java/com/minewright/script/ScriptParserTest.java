package com.minewright.script;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.time.Instant;

/**
 * Unit tests for {@link ScriptParser}.
 *
 * <p>Tests verify the DSL parser functionality:
 * <ul>
 *   <li>Parsing simple action nodes</li>
 *   <li>Parsing composite nodes (sequence, selector, parallel)</li>
 *   <li>Parsing control flow nodes (loop, if, condition)</li>
 *   <li>Parsing YAML-like format scripts</li>
 *   <li>Error handling for malformed input</li>
 *   <li>Parameter parsing</li>
 * </ul>
 */
@DisplayName("ScriptParser Tests")
class ScriptParserTest {

    @Test
    @DisplayName("Parse simple action node")
    void testParseSimpleAction() throws ScriptParser.ScriptParseException {
        String dsl = "mine";
        ScriptNode node = ScriptParser.parseNode(dsl);

        assertEquals(ScriptNode.NodeType.ACTION, node.getType(),
            "Should parse as ACTION node");
        assertEquals("mine", node.getAction(),
            "Action name should be 'mine'");
    }

    @Test
    @DisplayName("Parse action with parameters")
    void testParseActionWithParameters() throws ScriptParser.ScriptParseException {
        String dsl = "mine(block=oak_log, count=10)";
        ScriptNode node = ScriptParser.parseNode(dsl);

        assertEquals(ScriptNode.NodeType.ACTION, node.getType());
        assertEquals("mine", node.getAction());
        assertEquals("oak_log", node.getStringParameter("block"));
        assertEquals(10, node.getIntParameter("count", 0));
    }

    @Test
    @DisplayName("Parse action with string parameter")
    void testParseActionWithStringParameter() throws ScriptParser.ScriptParseException {
        String dsl = "move(target=\"player_base\")";
        ScriptNode node = ScriptParser.parseNode(dsl);

        assertEquals(ScriptNode.NodeType.ACTION, node.getType());
        assertEquals("move", node.getAction());
        assertEquals("player_base", node.getStringParameter("target"));
    }

    @Test
    @DisplayName("Parse sequence node with brace syntax")
    void testParseSequenceWithBraces() throws ScriptParser.ScriptParseException {
        String dsl = "sequence { mine, place }";
        ScriptNode node = ScriptParser.parseNode(dsl);

        assertEquals(ScriptNode.NodeType.SEQUENCE, node.getType());
        assertEquals(2, node.getChildren().size());
        assertEquals(ScriptNode.NodeType.ACTION, node.getChildren().get(0).getType());
        assertEquals(ScriptNode.NodeType.ACTION, node.getChildren().get(1).getType());
    }

    @Test
    @DisplayName("Parse selector node")
    void testParseSelector() throws ScriptParser.ScriptParseException {
        String dsl = "selector { mine, craft }";
        ScriptNode node = ScriptParser.parseNode(dsl);

        assertEquals(ScriptNode.NodeType.SELECTOR, node.getType());
        assertEquals(2, node.getChildren().size());
    }

    @Test
    @DisplayName("Parse parallel node")
    void testParseParallel() throws ScriptParser.ScriptParseException {
        String dsl = "parallel { mine, build }";
        ScriptNode node = ScriptParser.parseNode(dsl);

        assertEquals(ScriptNode.NodeType.PARALLEL, node.getType());
        assertEquals(2, node.getChildren().size());
    }

    @Test
    @DisplayName("Parse nested sequence")
    void testParseNestedSequence() throws ScriptParser.ScriptParseException {
        String dsl = "sequence { sequence { mine, place }, build }";
        ScriptNode node = ScriptParser.parseNode(dsl);

        assertEquals(ScriptNode.NodeType.SEQUENCE, node.getType());
        assertEquals(2, node.getChildren().size());

        ScriptNode firstChild = node.getChildren().get(0);
        assertEquals(ScriptNode.NodeType.SEQUENCE, firstChild.getType());
        assertEquals(2, firstChild.getChildren().size());
    }

    @Test
    @DisplayName("Parse loop node")
    void testParseLoop() throws ScriptParser.ScriptParseException {
        String dsl = "repeat 5 { mine }";
        ScriptNode node = ScriptParser.parseNode(dsl);

        assertEquals(ScriptNode.NodeType.LOOP, node.getType());
        assertEquals(5, node.getIntParameter("iterations", 0));
        assertEquals(1, node.getChildren().size());
    }

    @Test
    @DisplayName("Parse loop with parenthesis syntax")
    void testParseLoopWithParenthesis() throws ScriptParser.ScriptParseException {
        String dsl = "repeat(10) { place }";
        ScriptNode node = ScriptParser.parseNode(dsl);

        assertEquals(ScriptNode.NodeType.LOOP, node.getType());
        assertEquals(10, node.getIntParameter("iterations", 0));
    }

    @Test
    @DisplayName("Parse if node with then branch")
    void testParseIfThen() throws ScriptParser.ScriptParseException {
        String dsl = "if $has_wood { craft }";
        ScriptNode node = ScriptParser.parseNode(dsl);

        assertEquals(ScriptNode.NodeType.IF, node.getType());
        assertEquals("$has_wood", node.getCondition());
        assertEquals(1, node.getChildren().size());
    }

    @Test
    @DisplayName("Parse if node with then and else branches")
    void testParseIfThenElse() throws ScriptParser.ScriptParseException {
        String dsl = "if $has_wood { craft } else { mine }";
        ScriptNode node = ScriptParser.parseNode(dsl);

        assertEquals(ScriptNode.NodeType.IF, node.getType());
        assertEquals("$has_wood", node.getCondition());
        assertEquals(2, node.getChildren().size());
    }

    @Test
    @DisplayName("Parse if with parenthesis condition")
    void testParseIfWithParenthesis() throws ScriptParser.ScriptParseException {
        String dsl = "if (inventory_count > 10) { store }";
        ScriptNode node = ScriptParser.parseNode(dsl);

        assertEquals(ScriptNode.NodeType.IF, node.getType());
        assertEquals("inventory_count > 10", node.getCondition());
    }

    @Test
    @DisplayName("Parse condition node")
    void testParseCondition() throws ScriptParser.ScriptParseException {
        String dsl = "condition $has_tools";
        ScriptNode node = ScriptParser.parseNode(dsl);

        assertEquals(ScriptNode.NodeType.CONDITION, node.getType());
        assertEquals("$has_tools", node.getCondition());
    }

    @Test
    @DisplayName("Parse YAML-like format script")
    void testParseYAMLFormat() throws ScriptParser.ScriptParseException {
        String dsl = """
            metadata:
              id: test_script
              name: "Test Script"
            script:
              - type: ACTION
                action: mine
                params:
                  block: oak_log
                  count: 5
            """;

        Script script = ScriptParser.parse(dsl);

        assertNotNull(script);
        assertEquals("test_script", script.getId());
        assertEquals("Test Script", script.getName());

        ScriptNode rootNode = script.getScriptNode();
        assertEquals(ScriptNode.NodeType.ACTION, rootNode.getType());
        assertEquals("mine", rootNode.getAction());
    }

    @Test
    @DisplayName("Parse YAML with metadata and parameters")
    void testParseYAMLWithMetadataAndParameters() throws ScriptParser.ScriptParseException {
        String dsl = """
            metadata:
              id: gather_wood
              name: "Gather Wood"
              description: "Gather oak logs from forest"
            parameters:
              - name: count
                type: integer
                default: 10
            script:
              - type: ACTION
                action: gather
                params:
                  target: oak_log
                  amount: $count
            """;

        Script script = ScriptParser.parse(dsl);

        assertEquals("gather_wood", script.getId());
        assertEquals("Gather Wood", script.getName());
        assertEquals("Gather oak logs from forest", script.getMetadata().getDescription());

        assertTrue(script.getParameters().containsKey("count"));
        Script.Parameter countParam = script.getParameters().get("count");
        assertEquals("integer", countParam.getType());
        assertEquals(10, countParam.getDefaultValue());
    }

    @Test
    @DisplayName("Parse empty sequence")
    void testParseEmptySequence() throws ScriptParser.ScriptParseException {
        String dsl = "sequence { }";
        ScriptNode node = ScriptParser.parseNode(dsl);

        assertEquals(ScriptNode.NodeType.SEQUENCE, node.getType());
        assertTrue(node.getChildren().isEmpty(),
            "Empty sequence should have no children");
    }

    @Test
    @DisplayName("Parse boolean parameter values")
    void testParseBooleanParameters() throws ScriptParser.ScriptParseException {
        String dsl = "attack(aggressive=true, retreat=false)";
        ScriptNode node = ScriptParser.parseNode(dsl);

        assertEquals(true, node.getParameter("aggressive"));
        assertEquals(false, node.getParameter("retreat"));
    }

    @Test
    @DisplayName("Parse negative number parameter")
    void testParseNegativeNumberParameter() throws ScriptParser.ScriptParseException {
        String dsl = "move(offset=-5)";
        ScriptNode node = ScriptParser.parseNode(dsl);

        assertEquals(-5, node.getIntParameter("offset", 0));
    }

    @Test
    @DisplayName("Error on unterminated string")
    void testErrorOnUnterminatedString() {
        String dsl = "move(target=\"unclosed string";

        assertThrows(ScriptParser.ScriptParseException.class,
            () -> ScriptParser.parseNode(dsl),
            "Should throw exception for unterminated string");
    }

    @Test
    @DisplayName("Error on invalid syntax")
    void testErrorOnInvalidSyntax() {
        String dsl = "sequence { mine, }";

        assertThrows(ScriptParser.ScriptParseException.class,
            () -> ScriptParser.parseNode(dsl),
            "Should throw exception for trailing comma");
    }

    @Test
    @DisplayName("Error on unexpected content after node")
    void testErrorOnUnexpectedContent() {
        String dsl = "mine extra_content";

        assertThrows(ScriptParser.ScriptParseException.class,
            () -> ScriptParser.parseNode(dsl),
            "Should throw exception for unexpected content");
    }

    @Test
    @DisplayName("Parse script with default metadata when missing")
    void testParseScriptWithDefaultMetadata() throws ScriptParser.ScriptParseException {
        String dsl = "mine";
        Script script = ScriptParser.parse(dsl);

        assertNotNull(script.getId(),
            "Script should have auto-generated ID");
        assertEquals("Generated Script", script.getName(),
            "Script should have default name");
        assertNotNull(script.getMetadata(),
            "Script should have metadata");
    }

    @Test
    @DisplayName("Parse complex nested structure")
    void testParseComplexNestedStructure() throws ScriptParser.ScriptParseException {
        String dsl = """
            sequence {
              repeat 3 {
                sequence { mine, place }
              },
              if $has_cobblestone {
                craft
              } else {
                mine
              }
            }
            """;

        ScriptNode node = ScriptParser.parseNode(dsl);

        assertEquals(ScriptNode.NodeType.SEQUENCE, node.getType());
        assertEquals(2, node.getChildren().size());

        ScriptNode repeatChild = node.getChildren().get(0);
        assertEquals(ScriptNode.NodeType.LOOP, repeatChild.getType());
        assertEquals(3, repeatChild.getIntParameter("iterations", 0));

        ScriptNode ifChild = node.getChildren().get(1);
        assertEquals(ScriptNode.NodeType.IF, ifChild.getType());
        assertEquals(2, ifChild.getChildren().size());
    }

    @Test
    @DisplayName("Parse action node without parameters")
    void testParseActionWithoutParameters() throws ScriptParser.ScriptParseException {
        String dsl = "idle()";
        ScriptNode node = ScriptParser.parseNode(dsl);

        assertEquals(ScriptNode.NodeType.ACTION, node.getType());
        assertEquals("idle", node.getAction());
        assertTrue(node.getParameters().isEmpty(),
            "Action without parameters should have empty parameter map");
    }

    @Test
    @DisplayName("Parse quoted and unquoted string parameters")
    void testParseQuotedAndUnquotedStrings() throws ScriptParser.ScriptParseException {
        String dsl = "move(target=base, player=\"Steve\")";
        ScriptNode node = ScriptParser.parseNode(dsl);

        assertEquals("base", node.getStringParameter("target"));
        assertEquals("Steve", node.getStringParameter("player"));
    }

    @Test
    @DisplayName("Parse escape sequences in strings")
    void testParseEscapeSequences() throws ScriptParser.ScriptParseException {
        String dsl = "chat(message=\"Hello\\nWorld\")";
        ScriptNode node = ScriptParser.parseNode(dsl);

        String message = node.getStringParameter("message");
        assertTrue(message.contains("\n"),
            "Should parse newline escape sequence");
    }

    @Test
    @DisplayName("Parse zero iterations loop")
    void testParseZeroIterationsLoop() throws ScriptParser.ScriptParseException {
        String dsl = "repeat 0 { mine }";
        ScriptNode node = ScriptParser.parseNode(dsl);

        assertEquals(ScriptNode.NodeType.LOOP, node.getType());
        assertEquals(0, node.getIntParameter("iterations", 1));
    }

    @Test
    @DisplayName("Parse deeply nested nodes")
    void testParseDeeplyNestedNodes() throws ScriptParser.ScriptParseException {
        String dsl = "sequence { sequence { sequence { sequence { mine } } } }";
        ScriptNode node = ScriptParser.parseNode(dsl);

        assertEquals(ScriptNode.NodeType.SEQUENCE, node.getType());

        ScriptNode child = node.getChildren().get(0);
        assertEquals(ScriptNode.NodeType.SEQUENCE, child.getType());

        ScriptNode grandchild = child.getChildren().get(0);
        assertEquals(ScriptNode.NodeType.SEQUENCE, grandchild.getType());

        ScriptNode greatGrandchild = grandchild.getChildren().get(0);
        assertEquals(ScriptNode.NodeType.SEQUENCE, greatGrandchild.getType());
    }

    @Test
    @DisplayName("Parse selector with multiple actions")
    void testParseSelectorWithMultipleActions() throws ScriptParser.ScriptParseException {
        String dsl = "selector { mine(block=stone), craft(item=cobblestone), gather }";
        ScriptNode node = ScriptParser.parseNode(dsl);

        assertEquals(ScriptNode.NodeType.SELECTOR, node.getType());
        assertEquals(3, node.getChildren().size());
    }

    @Test
    @DisplayName("Error on missing required type in builder")
    void testErrorOnMissingTypeInBuilder() {
        ScriptNode.Builder builder = ScriptNode.builder();

        assertThrows(IllegalStateException.class,
            builder::build,
            "Builder should throw exception when type is not set");
    }

    @Test
    @DisplayName("Builder creates sequence helper")
    void testBuilderSequenceHelper() {
        ScriptNode child1 = ScriptNode.builder()
            .type(ScriptNode.NodeType.ACTION)
            .withAction("mine")
            .build();

        ScriptNode child2 = ScriptNode.builder()
            .type(ScriptNode.NodeType.ACTION)
            .withAction("place")
            .build();

        ScriptNode sequence = ScriptNode.Builder.sequence(child1, child2).build();

        assertEquals(ScriptNode.NodeType.SEQUENCE, sequence.getType());
        assertEquals(2, sequence.getChildren().size());
    }

    @Test
    @DisplayName("Builder creates selector helper")
    void testBuilderSelectorHelper() {
        ScriptNode child1 = ScriptNode.builder()
            .type(ScriptNode.NodeType.ACTION)
            .withAction("craft")
            .build();

        ScriptNode selector = ScriptNode.Builder.selector(child1).build();

        assertEquals(ScriptNode.NodeType.SELECTOR, selector.getType());
        assertEquals(1, selector.getChildren().size());
    }

    @Test
    @DisplayName("Builder creates parallel helper")
    void testBuilderParallelHelper() {
        ScriptNode child1 = ScriptNode.builder()
            .type(ScriptNode.NodeType.ACTION)
            .withAction("mine")
            .build();

        ScriptNode child2 = ScriptNode.builder()
            .type(ScriptNode.NodeType.ACTION)
            .withAction("build")
            .build();

        ScriptNode parallel = ScriptNode.Builder.parallel(child1, child2).build();

        assertEquals(ScriptNode.NodeType.PARALLEL, parallel.getType());
        assertEquals(2, parallel.getChildren().size());
    }

    @Test
    @DisplayName("Builder creates action helper")
    void testBuilderActionHelper() {
        Map<String, Object> params = Map.of("count", 10);
        ScriptNode action = ScriptNode.Builder.action("mine", params).build();

        assertEquals(ScriptNode.NodeType.ACTION, action.getType());
        assertEquals("mine", action.getAction());
        assertEquals(10, action.getIntParameter("count", 0));
    }

    @Test
    @DisplayName("Builder creates simple action helper")
    void testBuilderSimpleActionHelper() {
        ScriptNode action = ScriptNode.Builder.simpleAction("idle").build();

        assertEquals(ScriptNode.NodeType.ACTION, action.getType());
        assertEquals("idle", action.getAction());
        assertTrue(action.getParameters().isEmpty());
    }

    @Test
    @DisplayName("Builder creates condition helper")
    void testBuilderConditionHelper() {
        ScriptNode condition = ScriptNode.Builder.simpleCondition("$has_wood").build();

        assertEquals(ScriptNode.NodeType.CONDITION, condition.getType());
        assertEquals("$has_wood", condition.getCondition());
    }

    @Test
    @DisplayName("Builder creates loop helper")
    void testBuilderLoopHelper() {
        ScriptNode body = ScriptNode.builder()
            .type(ScriptNode.NodeType.ACTION)
            .withAction("mine")
            .build();

        ScriptNode loop = ScriptNode.Builder.loop(5, body).build();

        assertEquals(ScriptNode.NodeType.LOOP, loop.getType());
        assertEquals(5, loop.getIntParameter("iterations", 0));
        assertEquals(1, loop.getChildren().size());
    }

    @Test
    @DisplayName("Builder creates repeatUntil helper")
    void testBuilderRepeatUntilHelper() {
        ScriptNode body = ScriptNode.builder()
            .type(ScriptNode.NodeType.ACTION)
            .withAction("mine")
            .build();

        ScriptNode repeatUntil = ScriptNode.Builder.repeatUntil("$inventory_full", body).build();

        assertEquals(ScriptNode.NodeType.REPEAT_UNTIL, repeatUntil.getType());
        assertEquals("$inventory_full", repeatUntil.getCondition());
        assertEquals(1, repeatUntil.getChildren().size());
    }

    @Test
    @DisplayName("Builder creates ifElse helper")
    void testBuilderIfElseHelper() {
        ScriptNode thenBranch = ScriptNode.builder()
            .type(ScriptNode.NodeType.ACTION)
            .withAction("craft")
            .build();

        ScriptNode elseBranch = ScriptNode.builder()
            .type(ScriptNode.NodeType.ACTION)
            .withAction("mine")
            .build();

        ScriptNode ifElse = ScriptNode.Builder.ifElse("$has_wood", thenBranch, elseBranch).build();

        assertEquals(ScriptNode.NodeType.IF, ifElse.getType());
        assertEquals("$has_wood", ifElse.getCondition());
        assertEquals(2, ifElse.getChildren().size());
    }

    @Test
    @DisplayName("Builder creates ifElse without else branch")
    void testBuilderIfElseWithoutElse() {
        ScriptNode thenBranch = ScriptNode.builder()
            .type(ScriptNode.NodeType.ACTION)
            .withAction("craft")
            .build();

        ScriptNode ifElse = ScriptNode.Builder.ifElse("$has_wood", thenBranch, null).build();

        assertEquals(ScriptNode.NodeType.IF, ifElse.getType());
        assertEquals(1, ifElse.getChildren().size());
    }

    @Test
    @DisplayName("Node copy creates deep copy")
    void testNodeCopy() {
        ScriptNode original = ScriptNode.builder()
            .type(ScriptNode.NodeType.SEQUENCE)
            .addMetadata("key", "value")
            .addChild(ScriptNode.Builder.simpleAction("mine").build())
            .build();

        ScriptNode copy = original.copy();

        assertEquals(original.getType(), copy.getType());
        assertEquals(original.getMetadata(), copy.getMetadata());
        assertEquals(original.getChildren().size(), copy.getChildren().size());
    }

    @Test
    @DisplayName("Find nodes of specific type")
    void testFindNodesOfType() {
        ScriptNode node = ScriptNode.Builder.sequence(
            ScriptNode.Builder.simpleAction("mine").build(),
            ScriptNode.Builder.loop(5,
                ScriptNode.Builder.simpleAction("mine").build()
            ).build(),
            ScriptNode.Builder.simpleAction("craft").build()
        ).build();

        var loops = ScriptNode.findNodesOfType(node, ScriptNode.NodeType.LOOP);

        assertEquals(1, loops.size());
        assertEquals(ScriptNode.NodeType.LOOP, loops.get(0).getType());
    }

    @Test
    @DisplayName("Find nodes with specific action")
    void testFindNodesWithAction() {
        ScriptNode node = ScriptNode.Builder.sequence(
            ScriptNode.Builder.simpleAction("mine").build(),
            ScriptNode.Builder.loop(5,
                ScriptNode.Builder.simpleAction("mine").build()
            ).build(),
            ScriptNode.Builder.simpleAction("craft").build()
        ).build();

        var mineActions = ScriptNode.findNodesWithAction(node, "mine");

        assertEquals(2, mineActions.size());
    }

    @Test
    @DisplayName("Parse script with requirements section")
    void testParseScriptWithRequirements() throws ScriptParser.ScriptParseException {
        String dsl = """
            metadata:
              id: test_script
            requirements:
              inventory:
                - item: oak_log
                  quantity: 10
              tools:
                - diamond_axe
            script:
              - type: ACTION
                action: mine
            """;

        Script script = ScriptParser.parse(dsl);

        assertNotNull(script.getRequirements());
        assertEquals(1, script.getRequirements().getInventory().size());
        assertEquals("oak_log", script.getRequirements().getInventory().get(0).item());
        assertEquals(10, script.getRequirements().getInventory().get(0).quantity());
    }

    @Test
    @DisplayName("Parse script with error handlers")
    void testParseScriptWithErrorHandlers() throws ScriptParser.ScriptParseException {
        String dsl = """
            metadata:
              id: test_script
            script:
              - type: ACTION
                action: mine
            error_handling:
              out_of_stock:
                - type: ACTION
                  action: gather
              path_blocked:
                - type: ACTION
                  action: attack
            """;

        Script script = ScriptParser.parse(dsl);

        assertTrue(script.getErrorHandlers().containsKey("out_of_stock"));
        assertTrue(script.getErrorHandlers().containsKey("path_blocked"));

        var outOfStockHandlers = script.getErrorHandlers().get("out_of_stock");
        assertEquals(1, outOfStockHandlers.size());
        assertEquals(ScriptNode.NodeType.ACTION, outOfStockHandlers.get(0).getType());
    }

    @Test
    @DisplayName("Script toDSL conversion")
    void testScriptToDSL() throws ScriptParser.ScriptParseException {
        String dsl = """
            metadata:
              id: test_script
              name: Test Script
              description: A test script
            parameters:
              - name: count
                type: integer
                default: 5
            script:
              - type: ACTION
                action: mine
                params:
                  block: oak_log
                  count: $count
            """;

        Script script = ScriptParser.parse(dsl);
        String output = script.toDSL();

        assertTrue(output.contains("test_script"));
        assertTrue(output.contains("Test Script"));
        assertTrue(output.contains("A test script"));
        assertTrue(output.contains("count"));
        assertTrue(output.contains("mine"));
    }

    @Test
    @DisplayName("Parse indented YAML-like script")
    void testParseIndentedYAMLScript() throws ScriptParser.ScriptParseException {
        String dsl = """
            metadata:
              id: mining_script
            script:
              - type: SEQUENCE
                children:
                  - type: ACTION
                    action: mine
                    params:
                      block: stone
                      count: 10
                  - type: ACTION
                    action: craft
                    params:
                      item: cobblestone
            """;

        Script script = ScriptParser.parse(dsl);

        assertNotNull(script.getScriptNode());
        assertEquals(ScriptNode.NodeType.SEQUENCE, script.getScriptNode().getType());
        assertEquals(2, script.getScriptNode().getChildren().size());
    }

    @Test
    @DisplayName("Parse script with multiple parameters")
    void testParseMultipleParameters() throws ScriptParser.ScriptParseException {
        String dsl = """
            metadata:
              id: complex_script
            parameters:
              - name: target
                type: string
                default: oak_log
              - name: count
                type: integer
                default: 10
              - name: radius
                type: integer
                default: 5
            script:
              - type: ACTION
                action: gather
                params:
                  target: $target
                  amount: $count
                  range: $radius
            """;

        Script script = ScriptParser.parse(dsl);

        assertEquals(3, script.getParameters().size());
        assertTrue(script.getParameters().containsKey("target"));
        assertTrue(script.getParameters().containsKey("count"));
        assertTrue(script.getParameters().containsKey("radius"));
    }
}
