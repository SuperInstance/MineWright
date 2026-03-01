package com.minewright.profile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ProfileParser.
 *
 * <p>Tests cover:</p>
 * <ul>
 *   <li>JSON parsing from strings</li>
 *   <li>JSON parsing from files</li>
 *   <li>Profile validation</li>
 *   <li>Error handling for malformed JSON</li>
 *   <li>Task type conversion</li>
 *   <li>Conditions and parameters parsing</li>
 * </ul>
 *
 * @since 1.4.0
 */
@DisplayName("ProfileParser Tests")
class ProfileParserTest {

    private ProfileParser parser;

    @BeforeEach
    void setUp() {
        parser = new ProfileParser();
    }

    @Test
    @DisplayName("Parse valid profile from JSON string")
    void testParseValidProfileFromString() throws ProfileParser.ProfileParseException {
        String json = """
            {
              "name": "test_profile",
              "description": "Test description",
              "author": "Test Author",
              "version": "1.0.0",
              "tags": ["test", "example"],
              "tasks": [
                {
                  "type": "MINE",
                  "target": "iron_ore",
                  "quantity": 64,
                  "parameters": {
                    "radius": 32
                  }
                }
              ]
            }
            """;

        TaskProfile profile = parser.parse(json);

        assertNotNull(profile);
        assertEquals("test_profile", profile.getName());
        assertEquals("Test description", profile.getDescription());
        assertEquals("Test Author", profile.getAuthor());
        assertEquals("1.0.0", profile.getVersion());
        assertEquals(2, profile.getTags().size());
        assertEquals(1, profile.getTaskCount());
    }

    @Test
    @DisplayName("Parse profile with multiple tasks")
    void testParseProfileWithMultipleTasks() throws ProfileParser.ProfileParseException {
        String json = """
            {
              "name": "multi_task_profile",
              "tasks": [
                {
                  "type": "MINE",
                  "target": "iron_ore",
                  "quantity": 64
                },
                {
                  "type": "TRAVEL",
                  "target": "nearest_furnace"
                },
                {
                  "type": "CRAFT",
                  "target": "iron_ingot",
                  "quantity": 64
                }
              ]
            }
            """;

        TaskProfile profile = parser.parse(json);

        assertNotNull(profile);
        assertEquals(3, profile.getTaskCount());

        ProfileTask task1 = profile.getTasks().get(0);
        assertEquals(TaskType.MINE, task1.getType());
        assertEquals("iron_ore", task1.getTarget());
        assertEquals(64, task1.getQuantity());

        ProfileTask task2 = profile.getTasks().get(1);
        assertEquals(TaskType.TRAVEL, task2.getType());

        ProfileTask task3 = profile.getTasks().get(2);
        assertEquals(TaskType.CRAFT, task3.getType());
    }

    @Test
    @DisplayName("Parse profile with conditions")
    void testParseProfileWithConditions() throws ProfileParser.ProfileParseException {
        String json = """
            {
              "name": "conditional_profile",
              "tasks": [
                {
                  "type": "MINE",
                  "target": "iron_ore",
                  "quantity": 64,
                  "conditions": {
                    "inventory_has_space": true,
                    "daytime": true
                  }
                }
              ]
            }
            """;

        TaskProfile profile = parser.parse(json);

        assertNotNull(profile);
        ProfileTask task = profile.getTasks().get(0);
        assertEquals(2, task.getConditions().size());
        assertTrue(task.getConditionBoolean("inventory_has_space", false));
        assertTrue(task.getConditionBoolean("daytime", false));
    }

    @Test
    @DisplayName("Parse profile with settings")
    void testParseProfileWithSettings() throws ProfileParser.ProfileParseException {
        String json = """
            {
              "name": "settings_profile",
              "settings": {
                "repeat": true,
                "repeatCount": 5,
                "stopOnError": false,
                "maxRetries": 3,
                "retryDelayMs": 10000,
                "priority": 10
              },
              "tasks": [
                {
                  "type": "MINE",
                  "target": "stone",
                  "quantity": 64
                }
              ]
            }
            """;

        TaskProfile profile = parser.parse(json);

        assertNotNull(profile);
        TaskProfile.ProfileSettings settings = profile.getSettings();
        assertTrue(settings.isRepeat());
        assertEquals(5, settings.getRepeatCount());
        assertFalse(settings.isStopOnError());
        assertEquals(3, settings.getMaxRetries());
        assertEquals(10000, settings.getRetryDelayMs());
        assertEquals(10, settings.getPriority());
    }

    @Test
    @DisplayName("Parse profile with optional task")
    void testParseOptionalTask() throws ProfileParser.ProfileParseException {
        String json = """
            {
              "name": "optional_task_profile",
              "tasks": [
                {
                  "type": "MINE",
                  "target": "diamond_ore",
                  "quantity": 1,
                  "optional": true
                }
              ]
            }
            """;

        TaskProfile profile = parser.parse(json);

        assertNotNull(profile);
        ProfileTask task = profile.getTasks().get(0);
        assertTrue(task.isOptional());
    }

    @Test
    @DisplayName("Fail to parse profile with missing name")
    void testFailParseMissingName() {
        String json = """
            {
              "description": "No name",
              "tasks": [
                {
                  "type": "MINE",
                  "target": "stone",
                  "quantity": 64
                }
              ]
            }
            """;

        assertThrows(ProfileParser.ProfileParseException.class, () -> parser.parse(json));
    }

    @Test
    @DisplayName("Fail to parse profile with missing tasks")
    void testFailParseMissingTasks() {
        String json = """
            {
              "name": "no_tasks_profile"
            }
            """;

        assertThrows(ProfileParser.ProfileParseException.class, () -> parser.parse(json));
    }

    @Test
    @DisplayName("Fail to parse profile with empty tasks array")
    void testFailParseEmptyTasks() {
        String json = """
            {
              "name": "empty_tasks_profile",
              "tasks": []
            }
            """;

        assertThrows(ProfileParser.ProfileParseException.class, () -> parser.parse(json));
    }

    @Test
    @DisplayName("Fail to parse profile with invalid task type")
    void testFailParseInvalidTaskType() {
        String json = """
            {
              "name": "invalid_task_type",
              "tasks": [
                {
                  "type": "INVALID_TYPE",
                  "target": "stone"
                }
              ]
            }
            """;

        ProfileParser.ProfileParseException exception =
                assertThrows(ProfileParser.ProfileParseException.class, () -> parser.parse(json));
        assertTrue(exception.getMessage().contains("Invalid task type"));
    }

    @Test
    @DisplayName("Fail to parse invalid JSON")
    void testFailParseInvalidJson() {
        String json = """
            {
              "name": "invalid_json",
              "tasks": [
                {
                  "type": "MINE",
            }
            """;

        assertThrows(ProfileParser.ProfileParseException.class, () -> parser.parse(json));
    }

    @Test
    @DisplayName("Validate valid profile returns no errors")
    void testValidateValidProfile() throws ProfileParser.ProfileParseException {
        String json = """
            {
              "name": "valid_profile",
              "tasks": [
                {
                  "type": "MINE",
                  "target": "iron_ore",
                  "quantity": 64
                }
              ]
            }
            """;

        TaskProfile profile = parser.parse(json);
        List<String> errors = parser.validate(profile);

        assertTrue(errors.isEmpty());
    }

    @Test
    @DisplayName("Validate profile with missing target for required task type")
    void testValidateMissingTarget() throws ProfileParser.ProfileParseException {
        String json = """
            {
              "name": "missing_target_profile",
              "tasks": [
                {
                  "type": "MINE",
                  "quantity": 64
                }
              ]
            }
            """;

        TaskProfile profile = parser.parse(json);
        List<String> errors = parser.validate(profile);

        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("target is required")));
    }

    @Test
    @DisplayName("Validate profile with negative quantity")
    void testValidateNegativeQuantity() throws ProfileParser.ProfileParseException {
        String json = """
            {
              "name": "negative_quantity_profile",
              "tasks": [
                {
                  "type": "MINE",
                  "target": "iron_ore",
                  "quantity": -10
                }
              ]
            }
            """;

        TaskProfile profile = parser.parse(json);
        List<String> errors = parser.validate(profile);

        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("quantity cannot be negative")));
    }

    @Test
    @DisplayName("Convert ProfileTask to Task")
    void testProfileTaskToTaskConversion() throws ProfileParser.ProfileParseException {
        String json = """
            {
              "name": "conversion_test",
              "tasks": [
                {
                  "type": "MINE",
                  "target": "iron_ore",
                  "quantity": 64,
                  "parameters": {
                    "radius": 32
                  }
                }
              ]
            }
            """;

        TaskProfile profile = parser.parse(json);
        ProfileTask profileTask = profile.getTasks().get(0);

        com.minewright.action.Task task = profileTask.toTask();

        assertNotNull(task);
        assertEquals("mine", task.getAction());
        assertEquals("iron_ore", task.getParameter("target"));
        assertEquals(64, task.getParameter("quantity"));
        assertEquals(32, task.getParameter("radius"));
    }

    @Test
    @DisplayName("Profile toJson generates valid JSON structure")
    void testProfileToJson() throws ProfileParser.ProfileParseException {
        String json = """
            {
              "name": "tojson_test",
              "description": "Test to_json",
              "author": "Test",
              "version": "1.0.0",
              "tags": ["test"],
              "tasks": [
                {
                  "type": "MINE",
                  "target": "iron_ore",
                  "quantity": 64
                }
              ]
            }
            """;

        TaskProfile profile = parser.parse(json);
        String generatedJson = profile.toJson();

        assertNotNull(generatedJson);
        assertTrue(generatedJson.contains("\"name\": \"tojson_test\""));
        assertTrue(generatedJson.contains("\"type\": \"MINE\""));
        assertTrue(generatedJson.contains("\"target\": \"iron_ore\""));

        // Verify it can be parsed back
        TaskProfile reparsed = parser.parse(generatedJson);
        assertEquals(profile.getName(), reparsed.getName());
        assertEquals(profile.getTaskCount(), reparsed.getTaskCount());
    }

    @Test
    @DisplayName("TaskType fromString handles case insensitivity")
    void testTaskTypeFromStringCaseInsensitive() {
        assertEquals(TaskType.MINE, TaskType.fromString("mine"));
        assertEquals(TaskType.MINE, TaskType.fromString("MINE"));
        assertEquals(TaskType.MINE, TaskType.fromString("Mine"));
        assertEquals(TaskType.BUILD, TaskType.fromString("build"));
        assertEquals(TaskType.CRAFT, TaskType.fromString("CRAFT"));
    }

    @Test
    @DisplayName("TaskType fromString returns null for invalid type")
    void testTaskTypeFromStringInvalid() {
        assertNull(TaskType.fromString("invalid"));
        assertNull(TaskType.fromString(""));
        assertNull(TaskType.fromString(null));
    }

    @Test
    @DisplayName("TaskType requiresTarget returns correct values")
    void testTaskTypeRequiresTarget() {
        assertTrue(TaskType.MINE.requiresTarget());
        assertTrue(TaskType.BUILD.requiresTarget());
        assertTrue(TaskType.GATHER.requiresTarget());
        assertTrue(TaskType.CRAFT.requiresTarget());
        assertTrue(TaskType.TRAVEL.requiresTarget());
        assertTrue(TaskType.PLACE.requiresTarget());
        assertTrue(TaskType.ATTACK.requiresTarget());
        assertFalse(TaskType.FOLLOW.requiresTarget());
        assertFalse(TaskType.WAIT.requiresTarget());
    }
}
