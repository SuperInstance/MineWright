package com.minewright.script;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.List;

/**
 * Unit tests for {@link ScriptNode}.
 *
 * <p>Tests verify ScriptNode functionality:
 * <ul>
 *   <li>Builder pattern usage</li>
 *   <li>Node type properties</li>
 *   <li>Parameter handling</li>
 *   <li>Child node management</li>
 *   <li>Metadata handling</li>
 *   <li>Node copying</li>
 *   <li>Visitor pattern</li>
 * </ul>
 */
@DisplayName("ScriptNode Tests")
class ScriptNodeTest {

    @Test
    @DisplayName("Builder creates action node")
    void testBuilderCreatesActionNode() {
        ScriptNode node = ScriptNode.builder()
            .type(ScriptNode.NodeType.ACTION)
            .withAction("mine")
            .build();

        assertEquals(ScriptNode.NodeType.ACTION, node.getType());
        assertEquals("mine", node.getAction());
    }

    @Test
    @DisplayName("Builder creates condition node")
    void testBuilderCreatesConditionNode() {
        ScriptNode node = ScriptNode.builder()
            .type(ScriptNode.NodeType.CONDITION)
            .withCondition("$has_wood")
            .build();

        assertEquals(ScriptNode.NodeType.CONDITION, node.getType());
        assertEquals("$has_wood", node.getCondition());
    }

    @Test
    @DisplayName("Builder adds single parameter")
    void testBuilderAddsSingleParameter() {
        ScriptNode node = ScriptNode.builder()
            .type(ScriptNode.NodeType.ACTION)
            .addParameter("count", 10)
            .build();

        assertEquals(10, node.getIntParameter("count", 0));
    }

    @Test
    @DisplayName("Builder adds multiple parameters")
    void testBuilderAddsMultipleParameters() {
        Map<String, Object> params = Map.of(
            "block", "oak_log",
            "count", 5,
            "requireTool", true
        );

        ScriptNode node = ScriptNode.builder()
            .type(ScriptNode.NodeType.ACTION)
            .parameters(params)
            .build();

        assertEquals("oak_log", node.getStringParameter("block"));
        assertEquals(5, node.getIntParameter("count", 0));
        assertEquals(true, node.getParameter("requireTool"));
    }

    @Test
    @DisplayName("Builder adds single child")
    void testBuilderAddsSingleChild() {
        ScriptNode child = ScriptNode.builder()
            .type(ScriptNode.NodeType.ACTION)
            .withAction("mine")
            .build();

        ScriptNode parent = ScriptNode.builder()
            .type(ScriptNode.NodeType.SEQUENCE)
            .addChild(child)
            .build();

        assertEquals(1, parent.getChildren().size());
        assertEquals(child, parent.getChildren().get(0));
    }

    @Test
    @DisplayName("Builder adds multiple children")
    void testBuilderAddsMultipleChildren() {
        ScriptNode child1 = ScriptNode.builder()
            .type(ScriptNode.NodeType.ACTION)
            .withAction("mine")
            .build();

        ScriptNode child2 = ScriptNode.builder()
            .type(ScriptNode.NodeType.ACTION)
            .withAction("place")
            .build();

        ScriptNode parent = ScriptNode.builder()
            .type(ScriptNode.NodeType.SEQUENCE)
            .children(List.of(child1, child2))
            .build();

        assertEquals(2, parent.getChildren().size());
    }

    @Test
    @DisplayName("Builder adds metadata")
    void testBuilderAddsMetadata() {
        ScriptNode node = ScriptNode.builder()
            .type(ScriptNode.NodeType.ACTION)
            .addMetadata("author", "test")
            .addMetadata("version", "1.0")
            .build();

        assertEquals("test", node.getMetadata().get("author"));
        assertEquals("1.0", node.getMetadata().get("version"));
    }

    @Test
    @DisplayName("Builder throws exception without type")
    void testBuilderThrowsExceptionWithoutType() {
        assertThrows(IllegalStateException.class,
            ScriptNode.builder()::build,
            "Builder should throw exception when type is not set");
    }

    @Test
    @DisplayName("Node copy creates independent copy")
    void testNodeCopyCreatesIndependentCopy() {
        ScriptNode original = ScriptNode.builder()
            .type(ScriptNode.NodeType.SEQUENCE)
            .addParameter("key", "value")
            .addMetadata("meta", "data")
            .addChild(ScriptNode.Builder.simpleAction("mine").build())
            .build();

        ScriptNode copy = original.copy();

        // Verify copy has same values
        assertEquals(original.getType(), copy.getType());
        assertEquals(original.getParameters(), copy.getParameters());
        assertEquals(original.getMetadata(), copy.getMetadata());
        assertEquals(original.getChildren().size(), copy.getChildren().size());

        // Verify independence
        assertNotSame(original.getParameters(), copy.getParameters());
        assertNotSame(original.getMetadata(), copy.getMetadata());
        assertNotSame(original.getChildren(), copy.getChildren());
    }

    @Test
    @DisplayName("ToBuilder creates builder with same values")
    void testToBuilderCreatesBuilderWithSameValues() {
        ScriptNode original = ScriptNode.builder()
            .type(ScriptNode.NodeType.ACTION)
            .withAction("mine")
            .addParameter("count", 10)
            .addMetadata("key", "value")
            .build();

        ScriptNode copy = original.toBuilder().build();

        assertEquals(original.getType(), copy.getType());
        assertEquals(original.getAction(), copy.getAction());
        assertEquals(original.getParameters(), copy.getParameters());
        assertEquals(original.getMetadata(), copy.getMetadata());
    }

    @Test
    @DisplayName("GetParameter returns null for missing key")
    void testGetParameterReturnsNullForMissingKey() {
        ScriptNode node = ScriptNode.builder()
            .type(ScriptNode.NodeType.ACTION)
            .build();

        assertNull(node.getParameter("missingKey"));
    }

    @Test
    @DisplayName("GetStringParameter returns null for missing key")
    void testGetStringParameterReturnsNullForMissingKey() {
        ScriptNode node = ScriptNode.builder()
            .type(ScriptNode.NodeType.ACTION)
            .build();

        assertNull(node.getStringParameter("missingKey"));
    }

    @Test
    @DisplayName("GetIntParameter returns default for missing key")
    void testGetIntParameterReturnsDefaultForMissingKey() {
        ScriptNode node = ScriptNode.builder()
            .type(ScriptNode.NodeType.ACTION)
            .build();

        assertEquals(42, node.getIntParameter("missingKey", 42));
    }

    @Test
    @DisplayName("GetIntParameter converts Number to int")
    void testGetIntParameterConvertsNumberToInt() {
        ScriptNode node = ScriptNode.builder()
            .type(ScriptNode.NodeType.ACTION)
            .addParameter("value", 3.14)
            .build();

        assertEquals(3, node.getIntParameter("value", 0));
    }

    @Test
    @DisplayName("Builder sequence helper creates sequence")
    void testBuilderSequenceHelper() {
        ScriptNode child1 = ScriptNode.Builder.simpleAction("mine").build();
        ScriptNode child2 = ScriptNode.Builder.simpleAction("place").build();

        ScriptNode sequence = ScriptNode.Builder.sequence(child1, child2).build();

        assertEquals(ScriptNode.NodeType.SEQUENCE, sequence.getType());
        assertEquals(2, sequence.getChildren().size());
        assertEquals(child1.getType(), sequence.getChildren().get(0).getType());
        assertEquals(child2.getType(), sequence.getChildren().get(1).getType());
    }

    @Test
    @DisplayName("Builder selector helper creates selector")
    void testBuilderSelectorHelper() {
        ScriptNode child1 = ScriptNode.Builder.simpleAction("mine").build();
        ScriptNode child2 = ScriptNode.Builder.simpleAction("craft").build();

        ScriptNode selector = ScriptNode.Builder.selector(child1, child2).build();

        assertEquals(ScriptNode.NodeType.SELECTOR, selector.getType());
        assertEquals(2, selector.getChildren().size());
    }

    @Test
    @DisplayName("Builder parallel helper creates parallel")
    void testBuilderParallelHelper() {
        ScriptNode child1 = ScriptNode.Builder.simpleAction("mine").build();
        ScriptNode child2 = ScriptNode.Builder.simpleAction("build").build();

        ScriptNode parallel = ScriptNode.Builder.parallel(child1, child2).build();

        assertEquals(ScriptNode.NodeType.PARALLEL, parallel.getType());
        assertEquals(2, parallel.getChildren().size());
    }

    @Test
    @DisplayName("Builder action helper creates action with parameters")
    void testBuilderActionHelper() {
        Map<String, Object> params = Map.of("block", "stone", "count", 10);

        ScriptNode action = ScriptNode.Builder.action("mine", params).build();

        assertEquals(ScriptNode.NodeType.ACTION, action.getType());
        assertEquals("mine", action.getAction());
        assertEquals("stone", action.getStringParameter("block"));
        assertEquals(10, action.getIntParameter("count", 0));
    }

    @Test
    @DisplayName("Builder simpleAction helper creates action without parameters")
    void testBuilderSimpleActionHelper() {
        ScriptNode action = ScriptNode.Builder.simpleAction("idle").build();

        assertEquals(ScriptNode.NodeType.ACTION, action.getType());
        assertEquals("idle", action.getAction());
        assertTrue(action.getParameters().isEmpty());
    }

    @Test
    @DisplayName("Builder condition helper creates condition")
    void testBuilderConditionHelper() {
        ScriptNode condition = ScriptNode.Builder.simpleCondition("$has_wood").build();

        assertEquals(ScriptNode.NodeType.CONDITION, condition.getType());
        assertEquals("$has_wood", condition.getCondition());
    }

    @Test
    @DisplayName("Builder loop helper creates loop")
    void testBuilderLoopHelper() {
        ScriptNode body = ScriptNode.Builder.simpleAction("mine").build();

        ScriptNode loop = ScriptNode.Builder.loop(10, body).build();

        assertEquals(ScriptNode.NodeType.LOOP, loop.getType());
        assertEquals(10, loop.getIntParameter("iterations", 0));
        assertEquals(1, loop.getChildren().size());
        assertEquals(body.getType(), loop.getChildren().get(0).getType());
    }

    @Test
    @DisplayName("Builder repeatUntil helper creates repeatUntil")
    void testBuilderRepeatUntilHelper() {
        ScriptNode body = ScriptNode.Builder.simpleAction("mine").build();

        ScriptNode repeatUntil = ScriptNode.Builder.repeatUntil("$inventory_full", body).build();

        assertEquals(ScriptNode.NodeType.REPEAT_UNTIL, repeatUntil.getType());
        assertEquals("$inventory_full", repeatUntil.getCondition());
        assertEquals(1, repeatUntil.getChildren().size());
    }

    @Test
    @DisplayName("Builder ifElse helper creates if with both branches")
    void testBuilderIfElseHelperWithBothBranches() {
        ScriptNode thenBranch = ScriptNode.Builder.simpleAction("craft").build();
        ScriptNode elseBranch = ScriptNode.Builder.simpleAction("mine").build();

        ScriptNode ifNode = ScriptNode.Builder.ifElse("$has_wood", thenBranch, elseBranch).build();

        assertEquals(ScriptNode.NodeType.IF, ifNode.getType());
        assertEquals("$has_wood", ifNode.getCondition());
        assertEquals(2, ifNode.getChildren().size());
    }

    @Test
    @DisplayName("Builder ifElse helper creates if with only then branch")
    void testBuilderIfElseHelperWithOnlyThenBranch() {
        ScriptNode thenBranch = ScriptNode.Builder.simpleAction("craft").build();

        ScriptNode ifNode = ScriptNode.Builder.ifElse("$has_wood", thenBranch, null).build();

        assertEquals(ScriptNode.NodeType.IF, ifNode.getType());
        assertEquals("$has_wood", ifNode.getCondition());
        assertEquals(1, ifNode.getChildren().size());
    }

    @Test
    @DisplayName("Find nodes of type returns matching nodes")
    void testFindNodesOfType() {
        ScriptNode node = ScriptNode.Builder.sequence(
            ScriptNode.Builder.simpleAction("mine").build(),
            ScriptNode.Builder.loop(5,
                ScriptNode.Builder.simpleAction("place").build()
            ).build(),
            ScriptNode.Builder.sequence(
                ScriptNode.Builder.loop(3,
                    ScriptNode.Builder.simpleAction("craft").build()
                ).build()
            ).build()
        ).build();

        List<ScriptNode> loops = ScriptNode.findNodesOfType(node, ScriptNode.NodeType.LOOP);

        assertEquals(2, loops.size());
        assertTrue(loops.stream().allMatch(n -> n.getType() == ScriptNode.NodeType.LOOP));
    }

    @Test
    @DisplayName("Find nodes of type returns empty list when no matches")
    void testFindNodesOfTypeReturnsEmptyList() {
        ScriptNode node = ScriptNode.Builder.sequence(
            ScriptNode.Builder.simpleAction("mine").build(),
            ScriptNode.Builder.simpleAction("place").build()
        ).build();

        List<ScriptNode> loops = ScriptNode.findNodesOfType(node, ScriptNode.NodeType.LOOP);

        assertTrue(loops.isEmpty());
    }

    @Test
    @DisplayName("Find nodes with action returns matching nodes")
    void testFindNodesWithAction() {
        ScriptNode node = ScriptNode.Builder.sequence(
            ScriptNode.Builder.simpleAction("mine").build(),
            ScriptNode.Builder.loop(5,
                ScriptNode.Builder.simpleAction("mine").build()
            ).build(),
            ScriptNode.Builder.simpleAction("craft").build()
        ).build();

        List<ScriptNode> mineActions = ScriptNode.findNodesWithAction(node, "mine");

        assertEquals(2, mineActions.size());
        assertTrue(mineActions.stream().allMatch(n -> "mine".equals(n.getAction())));
    }

    @Test
    @DisplayName("Find nodes with action returns empty list when no matches")
    void testFindNodesWithActionReturnsEmptyList() {
        ScriptNode node = ScriptNode.Builder.sequence(
            ScriptNode.Builder.simpleAction("mine").build(),
            ScriptNode.Builder.simpleAction("place").build()
        ).build();

        List<ScriptNode> craftActions = ScriptNode.findNodesWithAction(node, "craft");

        assertTrue(craftActions.isEmpty());
    }

    @Test
    @DisplayName("Visitor traverses all nodes")
    void testVisitorTraversesAllNodes() {
        ScriptNode node = ScriptNode.Builder.sequence(
            ScriptNode.Builder.simpleAction("mine").build(),
            ScriptNode.Builder.sequence(
                ScriptNode.Builder.simpleAction("place").build(),
                ScriptNode.Builder.simpleAction("craft").build()
            ).build()
        ).build();

        final int[] visitCount = {0};
        ScriptNode.Visitor visitor = new ScriptNode.Visitor() {
            @Override
            public void visit(ScriptNode node) {
                visitCount[0]++;
            }
        };

        visitor.traverse(node);

        // Root sequence (1) + mine (1) + inner sequence (1) + place (1) + craft (1) = 5
        assertEquals(5, visitCount[0], "Visitor should traverse all 5 nodes");
    }

    @Test
    @DisplayName("Visitor can filter nodes by type")
    void testVisitorCanFilterNodesByType() {
        ScriptNode node = ScriptNode.Builder.sequence(
            ScriptNode.Builder.simpleAction("mine").build(),
            ScriptNode.Builder.sequence(
                ScriptNode.Builder.simpleAction("place").build()
            ).build()
        ).build();

        final int[] sequenceCount = {0};
        ScriptNode.Visitor visitor = new ScriptNode.Visitor() {
            @Override
            public void visit(ScriptNode n) {
                if (n.getType() == ScriptNode.NodeType.SEQUENCE) {
                    sequenceCount[0]++;
                }
            }
        };

        visitor.traverse(node);

        assertEquals(2, sequenceCount[0]); // root sequence + inner sequence
    }

    @Test
    @DisplayName("Node parameters are unmodifiable")
    void testNodeParametersAreUnmodifiable() {
        ScriptNode node = ScriptNode.builder()
            .type(ScriptNode.NodeType.ACTION)
            .addParameter("key", "value")
            .build();

        Map<String, Object> params = node.getParameters();

        assertThrows(UnsupportedOperationException.class,
            () -> params.put("newKey", "newValue"),
            "Parameters map should be unmodifiable");
    }

    @Test
    @DisplayName("Node children are unmodifiable")
    void testNodeChildrenAreUnmodifiable() {
        ScriptNode node = ScriptNode.builder()
            .type(ScriptNode.NodeType.SEQUENCE)
            .addChild(ScriptNode.Builder.simpleAction("mine").build())
            .build();

        List<ScriptNode> children = node.getChildren();

        assertThrows(UnsupportedOperationException.class,
            () -> children.add(ScriptNode.Builder.simpleAction("place").build()),
            "Children list should be unmodifiable");
    }

    @Test
    @DisplayName("Node metadata is unmodifiable")
    void testNodeMetadataIsUnmodifiable() {
        ScriptNode node = ScriptNode.builder()
            .type(ScriptNode.NodeType.ACTION)
            .addMetadata("key", "value")
            .build();

        Map<String, String> metadata = node.getMetadata();

        assertThrows(UnsupportedOperationException.class,
            () -> metadata.put("newKey", "newValue"),
            "Metadata map should be unmodifiable");
    }

    @Test
    @DisplayName("ToString includes type and action")
    void testToStringIncludesTypeAndAction() {
        ScriptNode node = ScriptNode.builder()
            .type(ScriptNode.NodeType.ACTION)
            .withAction("mine")
            .build();

        String str = node.toString();

        assertTrue(str.contains("ACTION"));
        assertTrue(str.contains("mine"));
    }

    @Test
    @DisplayName("ToString includes children count")
    void testToStringIncludesChildrenCount() {
        ScriptNode node = ScriptNode.builder()
            .type(ScriptNode.NodeType.SEQUENCE)
            .addChild(ScriptNode.Builder.simpleAction("mine").build())
            .addChild(ScriptNode.Builder.simpleAction("place").build())
            .build();

        String str = node.toString();

        assertTrue(str.contains("children=2"));
    }

    @Test
    @DisplayName("Empty children list is unmodifiable")
    void testEmptyChildrenListIsUnmodifiable() {
        ScriptNode node = ScriptNode.builder()
            .type(ScriptNode.NodeType.SEQUENCE)
            .build();

        List<ScriptNode> children = node.getChildren();

        assertThrows(UnsupportedOperationException.class,
            () -> children.add(ScriptNode.Builder.simpleAction("mine").build()),
            "Empty children list should be unmodifiable");
    }

    @Test
    @DisplayName("Null child is not added")
    void testNullChildIsNotAdded() {
        ScriptNode node = ScriptNode.builder()
            .type(ScriptNode.NodeType.SEQUENCE)
            .addChild(null)
            .build();

        assertTrue(node.getChildren().isEmpty(),
            "Null child should not be added");
    }

    @Test
    @DisplayName("All node types are supported")
    void testAllNodeTypesAreSupported() {
        for (ScriptNode.NodeType type : ScriptNode.NodeType.values()) {
            ScriptNode node = ScriptNode.builder()
                .type(type)
                .build();

            assertEquals(type, node.getType(),
                "Node type " + type + " should be supported");
        }
    }

    @Test
    @DisplayName("Builder can chain multiple operations")
    void testBuilderCanChainMultipleOperations() {
        ScriptNode child = ScriptNode.Builder.simpleAction("mine").build();

        ScriptNode node = ScriptNode.builder()
            .type(ScriptNode.NodeType.SEQUENCE)
            .addParameter("key1", "value1")
            .addParameter("key2", "value2")
            .addMetadata("meta1", "data1")
            .addMetadata("meta2", "data2")
            .addChild(child)
            .build();

        assertEquals(2, node.getParameters().size());
        assertEquals(2, node.getMetadata().size());
        assertEquals(1, node.getChildren().size());
    }
}
