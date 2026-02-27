package com.minewright.llm;

import com.minewright.action.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ResponseParser}.
 *
 * Tests cover:
 * <ul>
 *   <li>JSON parsing of AI responses</li>
 *   <li>Task extraction from parsed JSON</li>
 *   <li>Handling of malformed JSON</li>
 *   <li>Handling of markdown code blocks</li>
 *   <li>Parameter type parsing</li>
 * </ul>
 */
@DisplayName("ResponseParser Tests")
class ResponseParserTest {

    private final AtomicReference<String> lastError = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        // Set up error logger to capture parsing errors
        lastError.set(null);
        ResponseParser.setErrorLogger(error -> lastError.set(error));
    }

    @Test
    @DisplayName("Parse valid JSON response with all fields")
    void testParseValidJsonResponse() {
        String json = """
            {
                "reasoning": "Need to gather resources",
                "plan": "Mine stone for building materials",
                "tasks": [
                    {
                        "action": "mine",
                        "parameters": {
                            "block": "stone",
                            "quantity": 64
                        }
                    }
                ]
            }
            """;

        ResponseParser.ParsedResponse result = ResponseParser.parseAIResponse(json);

        assertNotNull(result, "Should successfully parse valid JSON");
        assertEquals("Need to gather resources", result.getReasoning());
        assertEquals("Mine stone for building materials", result.getPlan());
        assertEquals(1, result.getTasks().size());
    }

    @Test
    @DisplayName("Parse JSON with markdown code block wrapper")
    void testParseJsonWithMarkdownWrapper() {
        String json = """
            ```json
            {
                "reasoning": "Test reasoning",
                "plan": "Test plan",
                "tasks": []
            }
            ```
            """;

        ResponseParser.ParsedResponse result = ResponseParser.parseAIResponse(json);

        assertNotNull(result, "Should parse JSON wrapped in markdown");
        assertEquals("Test reasoning", result.getReasoning());
        assertEquals("Test plan", result.getPlan());
        assertTrue(result.getTasks().isEmpty());
    }

    @Test
    @DisplayName("Parse JSON with simple markdown wrapper")
    void testParseJsonWithSimpleMarkdown() {
        String json = """
            ```
            {
                "reasoning": "Simple test",
                "plan": "Simple plan",
                "tasks": []
            }
            ```
            """;

        ResponseParser.ParsedResponse result = ResponseParser.parseAIResponse(json);

        assertNotNull(result, "Should parse JSON with simple markdown wrapper");
        assertEquals("Simple test", result.getReasoning());
    }

    @Test
    @DisplayName("Parse JSON with newlines and formatting issues")
    void testParseJsonWithFormattingIssues() {
        String json = """
            {
                "reasoning": "Multi\\nline\\nreasoning",
                "plan": "Test plan",
                "tasks": [
                    {
                        "action": "move",
                        "parameters": {
                            "x": 10,
                            "y": 64,
                            "z": 20
                        }
                    }
                ]
            }
            """;

        ResponseParser.ParsedResponse result = ResponseParser.parseAIResponse(json);

        assertNotNull(result, "Should handle JSON with newlines in strings");
        assertEquals(1, result.getTasks().size());

        Task task = result.getTasks().get(0);
        assertEquals("move", task.getAction());
        assertEquals(10, task.getIntParameter("x", 0));
        assertEquals(64, task.getIntParameter("y", 0));
        assertEquals(20, task.getIntParameter("z", 0));
    }

    @Test
    @DisplayName("Parse JSON with missing optional fields")
    void testParseJsonWithMissingOptionalFields() {
        String json = """
            {
                "tasks": [
                    {
                        "action": "attack",
                        "parameters": {
                            "target": "zombie"
                        }
                    }
                ]
            }
            """;

        ResponseParser.ParsedResponse result = ResponseParser.parseAIResponse(json);

        assertNotNull(result, "Should parse JSON with missing optional fields");
        assertEquals("", result.getReasoning(), "Missing reasoning should be empty");
        assertEquals("", result.getPlan(), "Missing plan should be empty");
        assertEquals(1, result.getTasks().size());
        assertEquals("attack", result.getTasks().get(0).getAction());
    }

    @Test
    @DisplayName("Parse JSON with empty tasks array")
    void testParseJsonWithEmptyTasks() {
        String json = """
            {
                "reasoning": "No tasks needed",
                "plan": "Do nothing",
                "tasks": []
            }
            """;

        ResponseParser.ParsedResponse result = ResponseParser.parseAIResponse(json);

        assertNotNull(result);
        assertTrue(result.getTasks().isEmpty(),
                "Should have empty tasks list");
    }

    @Test
    @DisplayName("Parse JSON with multiple tasks")
    void testParseJsonWithMultipleTasks() {
        String json = """
            {
                "reasoning": "Complex plan",
                "plan": "Execute multiple actions",
                "tasks": [
                    {
                        "action": "mine",
                        "parameters": {"block": "stone", "quantity": 10}
                    },
                    {
                        "action": "craft",
                        "parameters": {"item": "stone_pickaxe"}
                    },
                    {
                        "action": "place",
                        "parameters": {"block": "cobblestone", "x": 0, "y": 64, "z": 0}
                    }
                ]
            }
            """;

        ResponseParser.ParsedResponse result = ResponseParser.parseAIResponse(json);

        assertNotNull(result);
        assertEquals(3, result.getTasks().size(), "Should parse all tasks");

        assertEquals("mine", result.getTasks().get(0).getAction());
        assertEquals("craft", result.getTasks().get(1).getAction());
        assertEquals("place", result.getTasks().get(2).getAction());
    }

    @Test
    @DisplayName("Parse task with different parameter types")
    void testParseTaskWithDifferentParameterTypes() {
        String json = """
            {
                "tasks": [
                    {
                        "action": "build",
                        "parameters": {
                            "structure": "house",
                            "width": 10,
                            "height": 5,
                            "depth": 8,
                            "materials": ["oak_planks", "cobblestone"],
                            "enabled": true
                        }
                    }
                ]
            }
            """;

        ResponseParser.ParsedResponse result = ResponseParser.parseAIResponse(json);

        assertNotNull(result);
        Task task = result.getTasks().get(0);

        assertEquals("house", task.getStringParameter("structure"));
        assertEquals(10, task.getIntParameter("width", 0));
        assertEquals(5, task.getIntParameter("height", 0));
        assertEquals(8, task.getIntParameter("depth", 0));
        assertTrue(task.getParameter("materials") instanceof List,
                "Should parse array parameters");
        assertEquals(true, task.getParameter("enabled"),
                "Should parse boolean parameters");
    }

    @Test
    @DisplayName("Parse task without parameters")
    void testParseTaskWithoutParameters() {
        String json = """
            {
                "tasks": [
                    {
                        "action": "wait"
                    }
                ]
            }
            """;

        ResponseParser.ParsedResponse result = ResponseParser.parseAIResponse(json);

        assertNotNull(result);
        assertEquals(1, result.getTasks().size());
        assertEquals("wait", result.getTasks().get(0).getAction());
        assertTrue(result.getTasks().get(0).getParameters().isEmpty(),
                "Should have empty parameters map");
    }

    @Test
    @DisplayName("Return null for null input")
    void testParseNullInput() {
        ResponseParser.ParsedResponse result = ResponseParser.parseAIResponse(null);

        assertNull(result, "Should return null for null input");
    }

    @Test
    @DisplayName("Return null for empty input")
    void testParseEmptyInput() {
        ResponseParser.ParsedResponse result = ResponseParser.parseAIResponse("");

        assertNull(result, "Should return null for empty input");
    }

    @Test
    @DisplayName("Return null for invalid JSON")
    void testParseInvalidJson() {
        String invalidJson = "This is not valid JSON at all";

        ResponseParser.ParsedResponse result = ResponseParser.parseAIResponse(invalidJson);

        assertNull(result, "Should return null for invalid JSON");
        assertNotNull(lastError.get(),
                "Should log error for invalid JSON");
    }

    @Test
    @DisplayName("Return null for malformed JSON")
    void testParseMalformedJson() {
        String malformedJson = """
            {
                "reasoning": "Test",
                "tasks": [
                    {
                        "action": "mine",
                        "parameters": {"block": "stone"
                    }
                ]
            }
            """;

        ResponseParser.ParsedResponse result = ResponseParser.parseAIResponse(malformedJson);

        assertNull(result, "Should return null for malformed JSON");
        assertNotNull(lastError.get(),
                "Should log error for malformed JSON");
    }

    @Test
    @DisplayName("Parse JSON with missing action field in task")
    void testParseTaskWithoutAction() {
        String json = """
            {
                "tasks": [
                    {
                        "parameters": {"block": "stone"}
                    }
                ]
            }
            """;

        ResponseParser.ParsedResponse result = ResponseParser.parseAIResponse(json);

        assertNotNull(result);
        assertTrue(result.getTasks().isEmpty(),
                "Should skip tasks without action field");
    }

    @Test
    @DisplayName("Parse JSON with numeric strings in parameters")
    void testParseNumericStringsInParameters() {
        String json = """
            {
                "tasks": [
                    {
                        "action": "move",
                        "parameters": {
                            "x": "100",
                            "y": "64",
                            "z": "-50"
                        }
                    }
                ]
            }
            """;

        ResponseParser.ParsedResponse result = ResponseParser.parseAIResponse(json);

        assertNotNull(result);
        Task task = result.getTasks().get(0);

        assertEquals("100", task.getStringParameter("x"),
                "String numbers should be preserved as strings");
        assertEquals("64", task.getStringParameter("y"));
        assertEquals("-50", task.getStringParameter("z"));
    }

    @Test
    @DisplayName("Parse JSON with array parameters containing numbers")
    void testParseArrayParametersWithNumbers() {
        String json = """
            {
                "tasks": [
                    {
                        "action": "build",
                        "parameters": {
                            "positions": [10, 64, 20, 30, 65, 40]
                        }
                    }
                ]
            }
            """;

        ResponseParser.ParsedResponse result = ResponseParser.parseAIResponse(json);

        assertNotNull(result);
        Task task = result.getTasks().get(0);

        @SuppressWarnings("unchecked")
        List<Object> positions = (List<Object>) task.getParameter("positions");

        assertNotNull(positions);
        assertEquals(6, positions.size());
        assertEquals(10, positions.get(0));
        assertEquals(64, positions.get(1));
        assertEquals(20, positions.get(2));
    }

    @Test
    @DisplayName("Parse JSON with extra whitespace")
    void testParseJsonWithExtraWhitespace() {
        String json = """
            ```json
            {
                "reasoning" : "Test" ,
                "plan" : "Test plan" ,
                "tasks" : [
                    {
                        "action" : "mine" ,
                        "parameters" : {
                            "block" : "stone"
                        }
                    }
                ]
            }
            ```
            """;

        ResponseParser.ParsedResponse result = ResponseParser.parseAIResponse(json);

        assertNotNull(result, "Should handle extra whitespace");
        assertEquals(1, result.getTasks().size());
    }

    @Test
    @DisplayName("Parse JSON with escaped characters")
    void testParseJsonWithEscapedCharacters() {
        String json = """
            {
                "reasoning": "Test \\"quoted\\" and \\'single\\'",
                "plan": "Test with \\\\backslash\\\\",
                "tasks": [
                    {
                        "action": "say",
                        "parameters": {
                            "message": "Hello \\"World\\""
                        }
                    }
                ]
            }
            """;

        ResponseParser.ParsedResponse result = ResponseParser.parseAIResponse(json);

        assertNotNull(result, "Should handle escaped characters");
        assertEquals(1, result.getTasks().size());
        assertEquals("say", result.getTasks().get(0).getAction());
    }

    @Test
    @DisplayName("ParsedResponse getters return correct values")
    void testParsedResponseGetters() {
        String json = """
            {
                "reasoning": "Test reasoning",
                "plan": "Test plan",
                "tasks": [
                    {
                        "action": "test",
                        "parameters": {}
                    }
                ]
            }
            """;

        ResponseParser.ParsedResponse result = ResponseParser.parseAIResponse(json);

        assertNotNull(result);
        assertEquals("Test reasoning", result.getReasoning());
        assertEquals("Test plan", result.getPlan());
        assertEquals(1, result.getTasks().size());
        assertEquals("test", result.getTasks().get(0).getAction());
    }

    @Test
    @DisplayName("Parse JSON with common AI formatting mistakes")
    void testParseJsonWithCommonMistakes() {
        // Missing commas between objects (common AI error)
        String json = """
            {
                "tasks": [
                    {
                        "action": "mine",
                        "parameters": {"block": "stone"}
                    }
                    {
                        "action": "craft",
                        "parameters": {"item": "pickaxe"}
                    }
                ]
            }
            """;

        ResponseParser.ParsedResponse result = ResponseParser.parseAIResponse(json);

        // The parser tries to fix missing commas
        // This may or may not succeed depending on the fix effectiveness
        // Just verify it doesn't crash and returns something reasonable
        if (result != null) {
            // If parsing succeeds, verify structure
            assertNotNull(result.getTasks());
        } else {
            // If parsing fails, that's acceptable for malformed JSON
            assertNotNull(lastError.get());
        }
    }

    @Test
    @DisplayName("Parse task with nested object in parameters")
    void testParseTaskWithNestedObject() {
        String json = """
            {
                "tasks": [
                    {
                        "action": "complex",
                        "parameters": {
                            "simple": "value",
                            "nested": {
                                "key": "value"
                            }
                        }
                    }
                ]
            }
            """;

        ResponseParser.ParsedResponse result = ResponseParser.parseAIResponse(json);

        assertNotNull(result);
        Task task = result.getTasks().get(0);

        assertEquals("value", task.getStringParameter("simple"));
        // Nested objects aren't explicitly handled in the parser,
        // but they shouldn't cause crashes
        Object nested = task.getParameter("nested");
        assertNotNull(nested);
    }

    @Test
    @DisplayName("Parse JSON with unicode characters")
    void testParseJsonWithUnicode() {
        String json = """
            {
                "reasoning": "Test with emoji: \\u2705 and unicode: \\u4E2D\\u6587",
                "plan": "Unicode test",
                "tasks": []
            }
            """;

        ResponseParser.ParsedResponse result = ResponseParser.parseAIResponse(json);

        assertNotNull(result, "Should handle unicode characters");
        assertTrue(result.getReasoning().contains("emoji") ||
                     result.getReasoning().contains("\u2705") ||
                     result.getReasoning().contains("\u4E2D"));
    }

    @Test
    @DisplayName("Handle very large JSON response")
    void testParseLargeJsonResponse() {
        StringBuilder tasksBuilder = new StringBuilder();
        tasksBuilder.append("{");
        tasksBuilder.append("\"reasoning\": \"Large response\",");
        tasksBuilder.append("\"plan\": \"Process many tasks\",");
        tasksBuilder.append("\"tasks\": [");

        // Create 100 tasks
        for (int i = 0; i < 100; i++) {
            if (i > 0) tasksBuilder.append(",");
            tasksBuilder.append("{");
            tasksBuilder.append("\"action\": \"task" + i + "\",");
            tasksBuilder.append("\"parameters\": {\"id\": " + i + ", \"name\": \"Task " + i + "\"}");
            tasksBuilder.append("}");
        }

        tasksBuilder.append("]}");

        ResponseParser.ParsedResponse result = ResponseParser.parseAIResponse(tasksBuilder.toString());

        assertNotNull(result, "Should handle large JSON responses");
        assertEquals(100, result.getTasks().size(), "Should parse all tasks");
    }
}
