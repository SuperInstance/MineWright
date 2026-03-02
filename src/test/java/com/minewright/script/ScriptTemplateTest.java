package com.minewright.script;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ScriptTemplate.
 */
class ScriptTemplateTest {

    @Test
    void testBuilderWithAllFields() {
        ScriptTemplate.TemplateMetadata metadata = new ScriptTemplate.TemplateMetadata(
            "test_id",
            "Test Template",
            "Test Description",
            "Test Author",
            "1.0.0",
            List.of("test", "example"),
            "test_category"
        );

        ScriptNode rootNode = ScriptNode.builder()
            .type(ScriptNode.NodeType.SEQUENCE)
            .addChild(ScriptNode.Builder.simpleAction("test_action").build())
            .build();

        ScriptTemplate template = ScriptTemplate.builder()
            .metadata(metadata)
            .rootNode(rootNode)
            .build();

        assertNotNull(template);
        assertEquals("test_id", template.getId());
        assertEquals("Test Template", template.getName());
        assertEquals("Test Description", template.getDescription());
        assertEquals("test_category", template.getMetadata().getCategory());
        assertEquals(2, template.getMetadata().getTags().size());
    }

    @Test
    void testBuilderWithoutMetadata() {
        ScriptNode rootNode = ScriptNode.builder()
            .type(ScriptNode.NodeType.SEQUENCE)
            .build();

        assertThrows(
            IllegalStateException.class,
            () -> ScriptTemplate.builder()
                .rootNode(rootNode)
                .build()
        );
    }

    @Test
    void testBuilderWithoutRootNode() {
        ScriptTemplate.TemplateMetadata metadata = new ScriptTemplate.TemplateMetadata(
            "test_id",
            "Test Template",
            "Test Description",
            "Test Author",
            "1.0.0",
            List.of(),
            "test_category"
        );

        assertThrows(
            IllegalStateException.class,
            () -> ScriptTemplate.builder()
                .metadata(metadata)
                .build()
        );
    }

    @Test
    void testInstantiateWithParameters() {
        ScriptTemplate.TemplateMetadata metadata = new ScriptTemplate.TemplateMetadata(
            "test_template",
            "Test Template",
            "Test Description",
            "Test Author",
            "1.0.0",
            List.of(),
            "test"
        );

        List<ScriptTemplate.TemplateParameter> parameters = new ArrayList<>();
        parameters.add(new ScriptTemplate.TemplateParameter(
            "block_type",
            "string",
            "stone",
            false,
            "Type of block",
            null
        ));
        parameters.add(new ScriptTemplate.TemplateParameter(
            "quantity",
            "integer",
            64,
            true,
            "Number of blocks",
            null
        ));

        ScriptNode rootNode = ScriptNode.builder()
            .type(ScriptNode.NodeType.SEQUENCE)
            .addChild(
                ScriptNode.Builder.action("mine", Map.of(
                    "block", "${block_type}",
                    "target", "${quantity}"
                )).build()
            )
            .build();

        ScriptTemplate template = ScriptTemplate.builder()
            .metadata(metadata)
            .parameters(parameters)
            .rootNode(rootNode)
            .build();

        Map<String, Object> params = new HashMap<>();
        params.put("block_type", "diamond_ore");
        params.put("quantity", 32);

        Script script = template.instantiate(params);

        assertNotNull(script);
        assertEquals("diamond_ore", script.getParameters().get("block_type").getDefaultValue());
        assertEquals(32, script.getParameters().get("quantity").getDefaultValue());

        // Check parameter substitution in nodes
        ScriptNode actionNode = script.getScriptNode().getChildren().get(0);
        assertEquals("diamond_ore", actionNode.getStringParameter("block"));
        assertEquals("32", actionNode.getStringParameter("target"));
    }

    @Test
    void testInstantiateWithDefaultParameters() {
        ScriptTemplate.TemplateMetadata metadata = new ScriptTemplate.TemplateMetadata(
            "default_template",
            "Default Template",
            "Description",
            "Author",
            "1.0.0",
            List.of(),
            "test"
        );

        List<ScriptTemplate.TemplateParameter> parameters = new ArrayList<>();
        parameters.add(new ScriptTemplate.TemplateParameter(
            "speed",
            "integer",
            5,
            false,
            "Movement speed",
            null
        ));

        ScriptNode rootNode = ScriptNode.builder()
            .type(ScriptNode.NodeType.SEQUENCE)
            .addChild(
                ScriptNode.Builder.action("move", Map.of("speed", "${speed}")).build()
            )
            .build();

        ScriptTemplate template = ScriptTemplate.builder()
            .metadata(metadata)
            .parameters(parameters)
            .rootNode(rootNode)
            .build();

        Script script = template.instantiate(); // No parameters provided

        assertNotNull(script);
        assertEquals(5, script.getParameters().get("speed").getDefaultValue());

        // Check default value substitution
        ScriptNode actionNode = script.getScriptNode().getChildren().get(0);
        assertEquals("5", actionNode.getStringParameter("speed"));
    }

    @Test
    void testInstantiateWithMissingRequiredParameter() {
        ScriptTemplate.TemplateMetadata metadata = new ScriptTemplate.TemplateMetadata(
            "required_template",
            "Required Template",
            "Description",
            "Author",
            "1.0.0",
            List.of(),
            "test"
        );

        List<ScriptTemplate.TemplateParameter> parameters = new ArrayList<>();
        parameters.add(new ScriptTemplate.TemplateParameter(
            "target",
            "string",
            null,
            true,
            "Required target",
            null
        ));

        ScriptNode rootNode = ScriptNode.builder()
            .type(ScriptNode.NodeType.SEQUENCE)
            .build();

        ScriptTemplate template = ScriptTemplate.builder()
            .metadata(metadata)
            .parameters(parameters)
            .rootNode(rootNode)
            .build();

        assertThrows(
            IllegalArgumentException.class,
            () -> template.instantiate(new HashMap<>())
        );
    }

    @Test
    void testInstantiateComplexBehaviorTree() {
        ScriptTemplate.TemplateMetadata metadata = new ScriptTemplate.TemplateMetadata(
            "complex_template",
            "Complex Template",
            "Description",
            "Author",
            "1.0.0",
            List.of(),
            "test"
        );

        List<ScriptTemplate.TemplateParameter> parameters = new ArrayList<>();
        parameters.add(new ScriptTemplate.TemplateParameter(
            "max_health",
            "integer",
            100,
            false,
            "Max health threshold",
            null
        ));

        // Build a complex tree with selectors, sequences, and conditions
        ScriptNode rootNode = ScriptNode.builder()
            .type(ScriptNode.NodeType.SELECTOR)
            .addChild(
                ScriptNode.Builder.simpleCondition("health_percent() > ${max_health}").build()
            )
            .addChild(
                ScriptNode.builder()
                    .type(ScriptNode.NodeType.SEQUENCE)
                    .addChild(ScriptNode.Builder.simpleAction("retreat").build())
                    .addChild(
                        ScriptNode.Builder.loop(3,
                            ScriptNode.Builder.simpleAction("scan").build()
                        ).build()
                    )
                    .build()
            )
            .build();

        ScriptTemplate template = ScriptTemplate.builder()
            .metadata(metadata)
            .parameters(parameters)
            .rootNode(rootNode)
            .build();

        Map<String, Object> params = new HashMap<>();
        params.put("max_health", 50);

        Script script = template.instantiate(params);

        assertNotNull(script);
        ScriptNode instantiatedRoot = script.getScriptNode();
        assertEquals(ScriptNode.NodeType.SELECTOR, instantiatedRoot.getType());

        // Check condition parameter substitution
        ScriptNode conditionNode = instantiatedRoot.getChildren().get(0);
        assertEquals("health_percent() > 50", conditionNode.getCondition());

        // Check loop parameter
        ScriptNode sequenceNode = instantiatedRoot.getChildren().get(1);
        ScriptNode loopNode = sequenceNode.getChildren().get(1);
        assertEquals(ScriptNode.NodeType.LOOP, loopNode.getType());
        assertEquals(3, loopNode.getIntParameter("iterations", 0));
    }

    @Test
    void testInstantiateWithErrorHandlers() {
        ScriptTemplate.TemplateMetadata metadata = new ScriptTemplate.TemplateMetadata(
            "error_template",
            "Error Template",
            "Description",
            "Author",
            "1.0.0",
            List.of(),
            "test"
        );

        ScriptNode rootNode = ScriptNode.builder()
            .type(ScriptNode.NodeType.SEQUENCE)
            .build();

        Map<String, List<ScriptNode>> errorHandlers = new HashMap<>();
        errorHandlers.put("no_tool", List.of(
            ScriptNode.Builder.simpleAction("announce").build()
        ));

        ScriptTemplate template = ScriptTemplate.builder()
            .metadata(metadata)
            .rootNode(rootNode)
            .errorHandlers(errorHandlers)
            .build();

        Script script = template.instantiate();

        assertNotNull(script);
        assertTrue(script.getErrorHandlers().containsKey("no_tool"));
        assertEquals(1, script.getErrorHandlers().get("no_tool").size());
    }

    @Test
    void testTemplateParameterBuilder() {
        ScriptTemplate.TemplateParameter param = new ScriptTemplate.TemplateParameter(
            "test_param",
            "string",
            "default",
            true,
            "Test parameter",
            List.of("value1", "value2", "value3")
        );

        assertEquals("test_param", param.getName());
        assertEquals("string", param.getType());
        assertEquals("default", param.getDefaultValue());
        assertTrue(param.isRequired());
        assertEquals("Test parameter", param.getDescription());
        assertEquals(3, param.getAllowedValues().size());
    }

    @Test
    void testTemplateRequirements() {
        List<ScriptTemplate.ItemRequirement> inventory = new ArrayList<>();
        inventory.add(new ScriptTemplate.ItemRequirement("pickaxe", 1));
        inventory.add(new ScriptTemplate.ItemRequirement("torch", 64));

        List<String> tools = List.of("pickaxe", "shovel");

        ScriptTemplate.TemplateRequirements requirements = new ScriptTemplate.TemplateRequirements(
            inventory,
            tools,
            600,
            100
        );

        assertEquals(2, requirements.getInventory().size());
        assertEquals(2, requirements.getTools().size());
        assertEquals(600, requirements.getMaxExecutionTime());
        assertEquals(100, requirements.getMaxDistance());
    }

    @Test
    void testTemplateTelemetry() {
        ScriptTemplate.TemplateTelemetry telemetry = new ScriptTemplate.TemplateTelemetry(
            "debug",
            List.of("execution_time", "blocks_mined", "distance_traveled")
        );

        assertEquals("debug", telemetry.getLogLevel());
        assertEquals(3, telemetry.getMetrics().size());
    }

    @Test
    void testInstantiationPreservesNodeTypes() {
        ScriptTemplate.TemplateMetadata metadata = new ScriptTemplate.TemplateMetadata(
            "node_types_template",
            "Node Types Template",
            "Description",
            "Author",
            "1.0.0",
            List.of(),
            "test"
        );

        ScriptNode rootNode = ScriptNode.builder()
            .type(ScriptNode.NodeType.PARALLEL)
            .addChild(ScriptNode.builder().type(ScriptNode.NodeType.CONDITION).build())
            .addChild(ScriptNode.builder().type(ScriptNode.NodeType.LOOP)
                .addParameter("iterations", 5)
                .addChild(ScriptNode.Builder.simpleAction("test").build())
                .build())
            .addChild(ScriptNode.builder().type(ScriptNode.NodeType.REPEAT_UNTIL)
                .withCondition("complete")
                .addChild(ScriptNode.Builder.simpleAction("work").build())
                .build())
            .build();

        ScriptTemplate template = ScriptTemplate.builder()
            .metadata(metadata)
            .rootNode(rootNode)
            .build();

        Script script = template.instantiate();

        assertEquals(ScriptNode.NodeType.PARALLEL, script.getScriptNode().getType());
        assertEquals(ScriptNode.NodeType.CONDITION, script.getScriptNode().getChildren().get(0).getType());
        assertEquals(ScriptNode.NodeType.LOOP, script.getScriptNode().getChildren().get(1).getType());
        assertEquals(ScriptNode.NodeType.REPEAT_UNTIL, script.getScriptNode().getChildren().get(2).getType());
    }

    @Test
    void testInstantiationWithNestedParameters() {
        ScriptTemplate.TemplateMetadata metadata = new ScriptTemplate.TemplateMetadata(
            "nested_template",
            "Nested Template",
            "Description",
            "Author",
            "1.0.0",
            List.of(),
            "test"
        );

        List<ScriptTemplate.TemplateParameter> parameters = new ArrayList<>();
        parameters.add(new ScriptTemplate.TemplateParameter("material", "string", "oak_planks", false, "Building material", null));
        parameters.add(new ScriptTemplate.TemplateParameter("size", "integer", 5, false, "Structure size", null));

        // Create nested structure: sequence -> loop -> action
        ScriptNode rootNode = ScriptNode.builder()
            .type(ScriptNode.NodeType.SEQUENCE)
            .addChild(
                ScriptNode.builder()
                    .type(ScriptNode.NodeType.LOOP)
                    .addParameter("iterations", "${size}")
                    .addChild(
                        ScriptNode.Builder.action("place", Map.of(
                            "block", "${material}",
                            "quantity", "1"
                        )).build()
                    )
                    .build()
            )
            .build();

        ScriptTemplate template = ScriptTemplate.builder()
            .metadata(metadata)
            .parameters(parameters)
            .rootNode(rootNode)
            .build();

        Map<String, Object> params = new HashMap<>();
        params.put("material", "stone_bricks");
        params.put("size", 10);

        Script script = template.instantiate(params);

        // Check nested parameter substitution
        ScriptNode loopNode = script.getScriptNode().getChildren().get(0);
        assertEquals("10", loopNode.getStringParameter("iterations"));

        ScriptNode actionNode = loopNode.getChildren().get(0);
        assertEquals("stone_bricks", actionNode.getStringParameter("block"));
        assertEquals("1", actionNode.getStringParameter("quantity"));
    }
}
