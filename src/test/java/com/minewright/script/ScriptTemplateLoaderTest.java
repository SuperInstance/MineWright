package com.minewright.script;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ScriptTemplateLoader.
 */
class ScriptTemplateLoaderTest {

    @TempDir
    Path tempDir;

    private static final String VALID_TEMPLATE_JSON = """
        {
          "metadata": {
            "id": "test_template",
            "name": "Test Template",
            "description": "A test template",
            "author": "Test Author",
            "version": "1.0.0",
            "tags": ["test"],
            "category": "test"
          },
          "parameters": [
            {
              "name": "test_param",
              "type": "string",
              "default": "default_value",
              "required": false,
              "description": "A test parameter"
            }
          ],
          "requirements": {
            "inventory": [],
            "tools": [],
            "maxExecutionTime": 300,
            "maxDistance": 100
          },
          "triggers": [
            {
              "type": "manual",
              "description": "Manual trigger"
            }
          ],
          "root_node": {
            "type": "sequence",
            "children": [
              {
                "type": "action",
                "action": "say",
                "parameters": {
                  "message": "Hello ${test_param}"
                }
              }
            ]
          },
          "error_handlers": {},
          "telemetry": {
            "logLevel": "info",
            "metrics": ["execution_time"]
          }
        }
        """;

    @BeforeEach
    void setUp() {
        ScriptTemplateLoader.setTemplatesDirectory(tempDir.toString());
        ScriptTemplateLoader.clearCache();
    }

    @Test
    void testLoadValidTemplate() throws Exception {
        // Create test template file
        Path templateFile = tempDir.resolve("test_template.json");
        Files.writeString(templateFile, VALID_TEMPLATE_JSON);

        // Load template
        ScriptTemplate template = ScriptTemplateLoader.load("test_template");

        assertNotNull(template);
        assertEquals("test_template", template.getId());
        assertEquals("Test Template", template.getName());
        assertEquals("A test template", template.getDescription());
        assertEquals("test", template.getMetadata().getCategory());

        // Check parameters
        assertEquals(1, template.getParameters().size());
        ScriptTemplate.TemplateParameter param = template.getParameters().get(0);
        assertEquals("test_param", param.getName());
        assertEquals("string", param.getType());
        assertEquals("default_value", param.getDefaultValue());
        assertFalse(param.isRequired());

        // Check root node
        assertNotNull(template.getRootNode());
        assertEquals(ScriptNode.NodeType.SEQUENCE, template.getRootNode().getType());
        assertEquals(1, template.getRootNode().getChildren().size());

        // Check child action node
        ScriptNode actionNode = template.getRootNode().getChildren().get(0);
        assertEquals(ScriptNode.NodeType.ACTION, actionNode.getType());
        assertEquals("say", actionNode.getAction());
        assertEquals("Hello ${test_param}", actionNode.getStringParameter("message"));
    }

    @Test
    void testLoadNonExistentTemplate() {
        ScriptTemplate template = ScriptTemplateLoader.load("non_existent");
        assertNull(template);
    }

    @Test
    void testLoadInvalidTemplate() throws Exception {
        // Create invalid template file
        Path templateFile = tempDir.resolve("invalid_template.json");
        Files.writeString(templateFile, "{ invalid json }");

        assertThrows(
            ScriptTemplateLoader.ScriptTemplateException.class,
            () -> ScriptTemplateLoader.load("invalid_template")
        );
    }

    @Test
    void testLoadTemplateWithMissingMetadata() throws Exception {
        String invalidJson = """
            {
              "root_node": {
                "type": "action",
                "action": "test"
              }
            }
            """;

        Path templateFile = tempDir.resolve("no_metadata.json");
        Files.writeString(templateFile, invalidJson);

        assertThrows(
            ScriptTemplateLoader.ScriptTemplateException.class,
            () -> ScriptTemplateLoader.load("no_metadata")
        );
    }

    @Test
    void testLoadAllTemplates() throws Exception {
        // Create multiple template files
        Files.writeString(tempDir.resolve("template1.json"), VALID_TEMPLATE_JSON.replace("test_template", "template1"));
        Files.writeString(tempDir.resolve("template2.json"), VALID_TEMPLATE_JSON.replace("test_template", "template2"));

        List<ScriptTemplate> templates = ScriptTemplateLoader.loadAll();

        assertEquals(2, templates.size());
        assertTrue(templates.stream().anyMatch(t -> t.getId().equals("template1")));
        assertTrue(templates.stream().anyMatch(t -> t.getId().equals("template2")));
    }

    @Test
    void testLoadByCategory() throws Exception {
        // Create templates with different categories
        String category1Json = VALID_TEMPLATE_JSON.replace("\"category\": \"test\"", "\"category\": \"mining\"");
        String category2Json = VALID_TEMPLATE_JSON.replace("\"category\": \"test\"", "\"category\": \"building\"");

        Files.writeString(tempDir.resolve("mining_template.json"), category1Json.replace("test_template", "mining_template"));
        Files.writeString(tempDir.resolve("building_template.json"), category2Json.replace("test_template", "building_template"));

        List<ScriptTemplate> miningTemplates = ScriptTemplateLoader.loadByCategory("mining");

        assertEquals(1, miningTemplates.size());
        assertEquals("mining", miningTemplates.get(0).getMetadata().getCategory());
    }

    @Test
    void testTemplateCaching() throws Exception {
        Path templateFile = tempDir.resolve("cached_template.json");
        Files.writeString(templateFile, VALID_TEMPLATE_JSON.replace("test_template", "cached_template"));

        // First load - from file
        ScriptTemplate template1 = ScriptTemplateLoader.load("cached_template");
        assertNotNull(template1);

        // Second load - from cache
        ScriptTemplate template2 = ScriptTemplateLoader.load("cached_template");
        assertNotNull(template2);

        // Should be same instance
        assertSame(template1, template2);
        assertTrue(ScriptTemplateLoader.isCached("cached_template"));
    }

    @Test
    void testTemplateReload() throws Exception {
        Path templateFile = tempDir.resolve("reload_template.json");
        Files.writeString(templateFile, VALID_TEMPLATE_JSON.replace("test_template", "reload_template"));

        ScriptTemplate template1 = ScriptTemplateLoader.load("reload_template");
        assertEquals("Test Template", template1.getName());

        // Modify file
        String modifiedJson = VALID_TEMPLATE_JSON
            .replace("test_template", "reload_template")
            .replace("Test Template", "Modified Template");
        Files.writeString(templateFile, modifiedJson);

        // Reload
        ScriptTemplate template2 = ScriptTemplateLoader.reload("reload_template");

        assertEquals("Modified Template", template2.getName());
    }

    @Test
    void testClearCache() throws Exception {
        Path templateFile = tempDir.resolve("cache_test.json");
        Files.writeString(templateFile, VALID_TEMPLATE_JSON.replace("test_template", "cache_test"));

        ScriptTemplateLoader.load("cache_test");
        assertTrue(ScriptTemplateLoader.isCached("cache_test"));

        ScriptTemplateLoader.clearCache();
        assertFalse(ScriptTemplateLoader.isCached("cache_test"));
    }

    @Test
    void testGetCachedTemplateIds() throws Exception {
        Files.writeString(tempDir.resolve("template1.json"), VALID_TEMPLATE_JSON.replace("test_template", "template1"));
        Files.writeString(tempDir.resolve("template2.json"), VALID_TEMPLATE_JSON.replace("test_template", "template2"));

        ScriptTemplateLoader.loadAll();

        var cachedIds = ScriptTemplateLoader.getCachedTemplateIds();
        assertTrue(cachedIds.contains("template1"));
        assertTrue(cachedIds.contains("template2"));
        assertEquals(2, cachedIds.size());
    }

    @Test
    void testInstantiateTemplateWithParameters() throws Exception {
        Path templateFile = tempDir.resolve("inst_template.json");
        Files.writeString(templateFile, VALID_TEMPLATE_JSON.replace("test_template", "inst_template"));

        ScriptTemplate template = ScriptTemplateLoader.load("inst_template");

        Map<String, Object> params = new HashMap<>();
        params.put("test_param", "custom_value");

        Script script = template.instantiate(params);

        assertNotNull(script);
        assertEquals("inst_template", script.getId().substring(0, 13)); // Starts with template ID

        // Check that parameter was substituted
        ScriptNode actionNode = script.getScriptNode().getChildren().get(0);
        assertEquals("Hello custom_value", actionNode.getStringParameter("message"));
    }

    @Test
    void testInstantiateTemplateWithDefaultParameters() throws Exception {
        Path templateFile = tempDir.resolve("default_template.json");
        Files.writeString(templateFile, VALID_TEMPLATE_JSON.replace("test_template", "default_template"));

        ScriptTemplate template = ScriptTemplateLoader.load("default_template");
        Script script = template.instantiate(); // No parameters provided

        // Should use default value
        ScriptNode actionNode = script.getScriptNode().getChildren().get(0);
        assertEquals("Hello default_value", actionNode.getStringParameter("message"));
    }

    @Test
    void testInstantiateTemplateWithMissingRequiredParameter() throws Exception {
        String requiredParamJson = VALID_TEMPLATE_JSON
            .replace("\"required\": false", "\"required\": true")
            .replace("test_template", "required_template");

        Path templateFile = tempDir.resolve("required_template.json");
        Files.writeString(templateFile, requiredParamJson);

        ScriptTemplate template = ScriptTemplateLoader.load("required_template");

        assertThrows(
            IllegalArgumentException.class,
            () -> template.instantiate(new HashMap<>())
        );
    }

    @Test
    void testComplexBehaviorTree() throws Exception {
        String complexJson = """
            {
              "metadata": {
                "id": "complex_tree",
                "name": "Complex Tree",
                "description": "A complex behavior tree",
                "author": "Test",
                "version": "1.0.0",
                "tags": ["test"],
                "category": "test"
              },
              "parameters": [],
              "requirements": {
                "inventory": [],
                "tools": [],
                "maxExecutionTime": 300,
                "maxDistance": 100
              },
              "triggers": [],
              "root_node": {
                "type": "selector",
                "children": [
                  {
                    "type": "condition",
                    "condition": "health_percent() > 50"
                  },
                  {
                    "type": "sequence",
                    "children": [
                      {
                        "type": "action",
                        "action": "retreat"
                      },
                      {
                        "type": "loop",
                        "parameters": {
                          "iterations": 3
                        },
                        "children": [
                          {
                            "type": "action",
                            "action": "scan"
                          }
                        ]
                      }
                    ]
                  }
                ]
              },
              "error_handlers": {},
              "telemetry": {
                "logLevel": "info",
                "metrics": []
              }
            }
            """;

        Path templateFile = tempDir.resolve("complex_tree.json");
        Files.writeString(templateFile, complexJson);

        ScriptTemplate template = ScriptTemplateLoader.load("complex_tree");

        assertNotNull(template);
        assertEquals(ScriptNode.NodeType.SELECTOR, template.getRootNode().getType());

        // Check first branch (condition)
        ScriptNode conditionNode = template.getRootNode().getChildren().get(0);
        assertEquals(ScriptNode.NodeType.CONDITION, conditionNode.getType());
        assertEquals("health_percent() > 50", conditionNode.getCondition());

        // Check second branch (sequence)
        ScriptNode sequenceNode = template.getRootNode().getChildren().get(1);
        assertEquals(ScriptNode.NodeType.SEQUENCE, sequenceNode.getType());
        assertEquals(2, sequenceNode.getChildren().size());

        // Check loop node
        ScriptNode loopNode = sequenceNode.getChildren().get(1);
        assertEquals(ScriptNode.NodeType.LOOP, loopNode.getType());
        assertEquals(3, loopNode.getIntParameter("iterations", 0));
    }

    @Test
    void testInvalidNodeId() throws Exception {
        String invalidIdJson = VALID_TEMPLATE_JSON.replace("\"id\": \"test_template\"", "\"id\": \"\"");

        Path templateFile = tempDir.resolve("no_id.json");
        Files.writeString(templateFile, invalidIdJson);

        assertThrows(
            ScriptTemplateLoader.ScriptTemplateException.class,
            () -> ScriptTemplateLoader.load("no_id")
        );
    }

    @Test
    void testSetTemplatesDirectory() {
        String newDir = tempDir.resolve("new_templates").toString();
        ScriptTemplateLoader.setTemplatesDirectory(newDir);
        assertEquals(newDir, ScriptTemplateLoader.getTemplatesDirectory());
    }

    @Test
    void testSetInvalidTemplatesDirectory() {
        assertThrows(
            IllegalArgumentException.class,
            () -> ScriptTemplateLoader.setTemplatesDirectory("")
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> ScriptTemplateLoader.setTemplatesDirectory(null)
        );
    }

    @Test
    void testLoadWithNullTemplateId() {
        assertThrows(
            IllegalArgumentException.class,
            () -> ScriptTemplateLoader.load(null)
        );
    }

    @Test
    void testLoadWithEmptyTemplateId() {
        assertThrows(
            IllegalArgumentException.class,
            () -> ScriptTemplateLoader.load("")
        );
    }
}
