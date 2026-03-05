package com.minewright.vision;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for StructuredVisionParser.
 * Tests parsing of structured JSON responses from vision models.
 */
@DisplayName("StructuredVisionParser Tests")
class StructuredVisionParserTest {

    private static final String VALID_JSON_RESPONSE = """
        {
            "biome": "plains",
            "terrainFeatures": "Flat grassland with scattered oak trees",
            "resources": "Oak trees, tall grass, flowers",
            "threats": "None visible",
            "suggestedActions": "Explore for villages, collect wood"
        }
        """;

    private static final String MARKDOWN_JSON_RESPONSE = """
        ```json
        {
            "biome": "forest",
            "terrainFeatures": "Dense woodland with hills",
            "resources": "Oak, birch, and dark oak trees",
            "threats": "Hostile mobs may spawn under trees",
            "suggestedActions": "Clear trees for building space"
        }
        ```
        """;

    private static final String GENERIC_CODE_BLOCK_RESPONSE = """
        ```
        {
            "biome": "desert",
            "terrainFeatures": "Sandy dunes with cacti",
            "resources": "Sand, cacti, occasional dead bushes",
            "threats": "Husks, heat exposure",
            "suggestedActions": "Find water, build shelter from heat"
        }
        ```
        """;

    private static final String NESTED_JSON_RESPONSE = """
        {
            "analysis": {
                "biome": "mountains",
                "elevation": "high"
            },
            "resources": {
                "wood": "spruce",
                "ore": "coal, iron"
            },
            "threats": ["falling", "cold"],
            "coordinates": {
                "x": 100,
                "y": 120,
                "z": 300
            }
        }
        """;

    private static final String INVALID_JSON_RESPONSE = """
        This is not valid JSON at all.
        Just plain text response.
        """;

    private static final String MALFORMED_JSON_RESPONSE = """
        {
            "biome": "swamp",
            "terrainFeatures": "Waterlogged terrain",
            "resources": ["lily pads", "vines"
        }
        """;

    private static final String EMPTY_RESPONSE = "";

    private static final String ARRAY_RESPONSE = """
        [
            {"biome": "plains"},
            {"biome": "forest"}
        ]
        """;

    @BeforeEach
    void setUp() {
        // Setup if needed
    }

    // ==================== Basic JSON Parsing Tests ====================

    @Test
    @DisplayName("Should parse valid JSON response")
    void testParseJsonResponse_Valid() {
        Map<String, Object> result = StructuredVisionParser.parseJsonResponse(VALID_JSON_RESPONSE);

        assertNotNull(result, "Result should not be null");
        assertEquals("plains", result.get("biome"), "Should parse biome field");
        assertEquals("Flat grassland with scattered oak trees", result.get("terrainFeatures"));
        assertEquals("Oak trees, tall grass, flowers", result.get("resources"));
        assertEquals("None visible", result.get("threats"));
        assertEquals("Explore for villages, collect wood", result.get("suggestedActions"));
    }

    @Test
    @DisplayName("Should parse JSON from markdown code block with json marker")
    void testParseJsonResponse_MarkdownJson() {
        Map<String, Object> result = StructuredVisionParser.parseJsonResponse(MARKDOWN_JSON_RESPONSE);

        assertNotNull(result, "Should extract JSON from markdown code block");
        assertEquals("forest", result.get("biome"), "Should parse biome from markdown");
        assertEquals("Dense woodland with hills", result.get("terrainFeatures"));
    }

    @Test
    @DisplayName("Should parse JSON from generic code block")
    void testParseJsonResponse_GenericCodeBlock() {
        Map<String, Object> result = StructuredVisionParser.parseJsonResponse(GENERIC_CODE_BLOCK_RESPONSE);

        assertNotNull(result, "Should extract JSON from generic code block");
        assertEquals("desert", result.get("biome"), "Should parse biome from code block");
    }

    @Test
    @DisplayName("Should parse nested JSON structures")
    void testParseJsonResponse_Nested() {
        Map<String, Object> result = StructuredVisionParser.parseJsonResponse(NESTED_JSON_RESPONSE);

        assertNotNull(result, "Should parse nested JSON");
        assertTrue(result.containsKey("analysis"), "Should contain analysis object");
        assertTrue(result.containsKey("resources"), "Should contain resources object");
        assertTrue(result.containsKey("threats"), "Should contain threats array");
        assertTrue(result.containsKey("coordinates"), "Should contain coordinates object");
    }

    @Test
    @DisplayName("Should throw exception for invalid JSON")
    void testParseJsonResponse_Invalid() {
        assertThrows(
            RuntimeException.class,
            () -> StructuredVisionParser.parseJsonResponse(INVALID_JSON_RESPONSE),
            "Should throw exception for invalid JSON"
        );
    }

    @Test
    @DisplayName("Should throw exception for malformed JSON")
    void testParseJsonResponse_Malformed() {
        assertThrows(
            RuntimeException.class,
            () -> StructuredVisionParser.parseJsonResponse(MALFORMED_JSON_RESPONSE),
            "Should throw exception for malformed JSON"
        );
    }

    @Test
    @DisplayName("Should throw exception for empty response")
    void testParseJsonResponse_Empty() {
        assertThrows(
            RuntimeException.class,
            () -> StructuredVisionParser.parseJsonResponse(EMPTY_RESPONSE),
            "Should throw exception for empty response"
        );
    }

    @Test
    @DisplayName("Should handle JSON array response")
    void testParseJsonResponse_Array() {
        Map<String, Object> result = StructuredVisionParser.parseJsonResponse(ARRAY_RESPONSE);

        assertNotNull(result, "Should parse JSON array");
        assertTrue(result instanceof Map, "Should convert array to map");
    }

    // ==================== Biome Analysis Parsing Tests ====================

    @Test
    @DisplayName("Should parse biome analysis response")
    void testParseBiomeAnalysis_Valid() {
        StructuredVisionParser.BiomeAnalysis analysis =
            StructuredVisionParser.parseBiomeAnalysis(VALID_JSON_RESPONSE);

        assertNotNull(analysis, "Analysis should not be null");
        assertEquals("plains", analysis.biome(), "Biome should match");
        assertEquals("Flat grassland with scattered oak trees", analysis.terrainFeatures());
        assertEquals("Oak trees, tall grass, flowers", analysis.resources());
        assertEquals("None visible", analysis.threats());
        assertEquals("Explore for villages, collect wood", analysis.suggestedActions());
    }

    @Test
    @DisplayName("Should parse biome analysis from markdown")
    void testParseBiomeAnalysis_Markdown() {
        StructuredVisionParser.BiomeAnalysis analysis =
            StructuredVisionParser.parseBiomeAnalysis(MARKDOWN_JSON_RESPONSE);

        assertNotNull(analysis, "Should parse from markdown");
        assertEquals("forest", analysis.biome());
        assertEquals("Dense woodland with hills", analysis.terrainFeatures());
    }

    @Test
    @DisplayName("Should throw exception for incomplete biome analysis")
    void testParseBiomeAnalysis_Incomplete() {
        String incomplete = """
            {
                "biome": "taiga"
            }
            """;

        assertThrows(
            RuntimeException.class,
            () -> StructuredVisionParser.parseBiomeAnalysis(incomplete),
            "Should throw exception for incomplete analysis"
        );
    }

    @Test
    @DisplayName("Should handle null values in biome analysis")
    void testParseBiomeAnalysis_NullValues() {
        String withNulls = """
            {
                "biome": "jungle",
                "terrainFeatures": null,
                "resources": null,
                "threats": null,
                "suggestedActions": null
            }
            """;

        StructuredVisionParser.BiomeAnalysis analysis =
            StructuredVisionParser.parseBiomeAnalysis(withNulls);

        assertNotNull(analysis);
        assertEquals("jungle", analysis.biome());
        assertNull(analysis.terrainFeatures());
        assertNull(analysis.resources());
        assertNull(analysis.threats());
        assertNull(analysis.suggestedActions());
    }

    // ==================== Edge Case Tests ====================

    @Test
    @DisplayName("Should handle JSON with extra whitespace")
    void testParseJsonResponse_ExtraWhitespace() {
        String whitespace = """
        \n\n
            {
                "biome": "plains"
            }
        \n\n
        """;

        Map<String, Object> result = StructuredVisionParser.parseJsonResponse(whitespace);

        assertNotNull(result);
        assertEquals("plains", result.get("biome"));
    }

    @Test
    @DisplayName("Should handle JSON with comments (non-standard)")
    void testParseJsonResponse_Comments() {
        // Standard JSON doesn't support comments, but models sometimes include them
        // This tests robustness
        String withComments = """
            {
                // This is the biome
                "biome": "plains",
                /* Multi-line
                   comment */
                "resources": "trees"
            }
            """;

        // Gson doesn't support comments, so this should fail
        // But we test the behavior
        assertThrows(
            RuntimeException.class,
            () -> StructuredVisionParser.parseJsonResponse(withComments),
            "Should reject JSON with comments"
        );
    }

    @Test
    @DisplayName("Should handle escaped characters in JSON")
    void testParseJsonResponse_EscapedCharacters() {
        String escaped = """
            {
                "biome": "plains",
                "description": "Line 1\\nLine 2\\tTabbed",
                "quote": "He said \\"hello\\""
            }
            """;

        Map<String, Object> result = StructuredVisionParser.parseJsonResponse(escaped);

        assertNotNull(result);
        assertEquals("Line 1\\nLine 2\\tTabbed", result.get("description"));
        assertEquals("He said \"hello\"", result.get("quote"));
    }

    @Test
    @DisplayName("Should handle Unicode characters in JSON")
    void testParseJsonResponse_Unicode() {
        String unicode = """
            {
                "biome": "plains",
                "text": "Japanese: 日本語, Emoji: 🏕️, Symbols: ♥♦♣♠"
            }
            """;

        Map<String, Object> result = StructuredVisionParser.parseJsonResponse(unicode);

        assertNotNull(result);
        assertTrue(result.get("text").toString().contains("日本語"));
        assertTrue(result.get("text").toString().contains("🏕️"));
    }

    @Test
    @DisplayName("Should handle very long JSON response")
    void testParseJsonResponse_LongJson() {
        StringBuilder longJson = new StringBuilder("{");
        for (int i = 0; i < 100; i++) {
            longJson.append("\"field").append(i).append("\": \"value").append(i).append("\"");
            if (i < 99) longJson.append(", ");
        }
        longJson.append("}");

        Map<String, Object> result = StructuredVisionParser.parseJsonResponse(longJson.toString());

        assertNotNull(result);
        assertEquals(100, result.size());
    }

    @Test
    @DisplayName("Should handle nested code blocks")
    void testParseJsonResponse_NestedCodeBlocks() {
        String nested = """
        Text before

        ```json
        {
            "biome": "plains"
        }
        ```

        Text after
        """;

        Map<String, Object> result = StructuredVisionParser.parseJsonResponse(nested);

        assertNotNull(result);
        assertEquals("plains", result.get("biome"));
    }

    // ==================== Type Conversion Tests ====================

    @Test
    @DisplayName("Should parse different data types correctly")
    void testParseJsonResponse_DataTypes() {
        String mixedTypes = """
            {
                "stringField": "text",
                "numberField": 42,
                "floatField": 3.14,
                "boolField": true,
                "nullField": null,
                "arrayField": [1, 2, 3],
                "objectField": {"nested": "value"}
            }
            """;

        Map<String, Object> result = StructuredVisionParser.parseJsonResponse(mixedTypes);

        assertEquals("text", result.get("stringField"));
        assertEquals(42.0, ((Number) result.get("numberField")).doubleValue(), 0.001);
        assertEquals(3.14, ((Number) result.get("floatField")).doubleValue(), 0.001);
        assertEquals(true, result.get("boolField"));
        assertNull(result.get("nullField"));
        assertTrue(result.get("arrayField") instanceof java.util.List);
        assertTrue(result.get("objectField") instanceof Map);
    }

    // ==================== Record Tests ====================

    @Test
    @DisplayName("BiomeAnalysis record should have correct values")
    void testBiomeAnalysisRecord() {
        StructuredVisionParser.BiomeAnalysis analysis =
            new StructuredVisionParser.BiomeAnalysis(
                "plains",
                "flat",
                "trees",
                "none",
                "explore"
            );

        assertEquals("plains", analysis.biome());
        assertEquals("flat", analysis.terrainFeatures());
        assertEquals("trees", analysis.resources());
        assertEquals("none", analysis.threats());
        assertEquals("explore", analysis.suggestedActions());
    }

    @Test
    @DisplayName("BiomeAnalysis record should implement equals correctly")
    void testBiomeAnalysisEquals() {
        StructuredVisionParser.BiomeAnalysis analysis1 =
            new StructuredVisionParser.BiomeAnalysis("plains", "flat", "trees", "none", "explore");

        StructuredVisionParser.BiomeAnalysis analysis2 =
            new StructuredVisionParser.BiomeAnalysis("plains", "flat", "trees", "none", "explore");

        StructuredVisionParser.BiomeAnalysis analysis3 =
            new StructuredVisionParser.BiomeAnalysis("forest", "hilly", "wood", "mobs", "build");

        assertEquals(analysis1, analysis2, "Equal analyses should be equal");
        assertNotEquals(analysis1, analysis3, "Different analyses should not be equal");
    }

    // ==================== Error Message Tests ====================

    @Test
    @DisplayName("Should provide helpful error message for invalid JSON")
    void testParseJsonResponse_ErrorMessage() {
        try {
            StructuredVisionParser.parseJsonResponse(INVALID_JSON_RESPONSE);
            fail("Should have thrown exception");
        } catch (RuntimeException e) {
            assertTrue(
                e.getMessage().contains("Failed to parse structured response"),
                "Error message should indicate parsing failure"
            );
        }
    }

    @Test
    @DisplayName("Should include original error in exception message")
    void testParseJsonResponse_OriginalError() {
        try {
            StructuredVisionParser.parseJsonResponse(MALFORMED_JSON_RESPONSE);
            fail("Should have thrown exception");
        } catch (RuntimeException e) {
            // Should wrap the original JSON parsing exception
            assertNotNull(e.getCause(), "Should have cause exception");
        }
    }

    // ==================== Null and Empty Input Tests ====================

    @Test
    @DisplayName("Should throw exception for null input")
    void testParseJsonResponse_Null() {
        assertThrows(
            RuntimeException.class,
            () -> StructuredVisionParser.parseJsonResponse(null),
            "Should throw exception for null input"
        );
    }

    @Test
    @DisplayName("Should throw exception for whitespace only")
    void testParseJsonResponse_WhitespaceOnly() {
        assertThrows(
            RuntimeException.class,
            () -> StructuredVisionParser.parseJsonResponse("   \n\n   "),
            "Should throw exception for whitespace only"
        );
    }

    // ==================== Practical Minecraft Response Tests ====================

    @Test
    @DisplayName("Should parse real Minecraft biome analysis")
    void testRealMinecraftResponse() {
        String realResponse = """
            Based on the screenshot, here is my analysis:

            ```json
            {
                "biome": "Plains",
                "terrainFeatures": "Flat grassland with gentle hills, nearby river",
                "resources": "Oak trees, tall grass, flowers, dirt, stone",
                "threats": "No immediate threats visible. Clear sight lines.",
                "suggestedActions": "Collect wood for tools, explore river for clay, watch for hostile mobs at night"
            }
            ```

            This area looks good for a base.
            """;

        StructuredVisionParser.BiomeAnalysis analysis =
            StructuredVisionParser.parseBiomeAnalysis(realResponse);

        assertNotNull(analysis);
        assertEquals("Plains", analysis.biome());
        assertTrue(analysis.resources().contains("oak"));
        assertTrue(analysis.suggestedActions().contains("wood"));
    }

    @Test
    @DisplayName("Should parse OCR text extraction response")
    void testOcrResponse() {
        String ocrResponse = """
            {
                "textType": "sign",
                "content": "Welcome to Steve's Farm!\n- Wood shop left\n- Food shop right",
                "confidence": 0.95,
                "position": {"x": 100, "y": 64, "z": 200}
            }
            """;

        Map<String, Object> result = StructuredVisionParser.parseJsonResponse(ocrResponse);

        assertNotNull(result);
        assertEquals("sign", result.get("textType"));
        assertTrue(result.get("content").toString().contains("Steve's Farm"));
        assertEquals(0.95, ((Number) result.get("confidence")).doubleValue(), 0.001);
    }

    @Test
    @DisplayName("Should parse build verification response")
    void testBuildVerificationResponse() {
        String buildResponse = """
            {
                "structureType": "house",
                "status": "incomplete",
                "correctBlocks": 85,
                "incorrectBlocks": 5,
                "missingBlocks": 10,
                "issues": [
                    {"location": "roof", "problem": "missing blocks"},
                    {"location": "door", "problem": "wrong block type"}
                ],
                "suggestions": ["Complete roof", "Replace door with oak door"]
            }
            """;

        Map<String, Object> result = StructuredVisionParser.parseJsonResponse(buildResponse);

        assertNotNull(result);
        assertEquals("house", result.get("structureType"));
        assertEquals("incomplete", result.get("status"));
        assertEquals(85, ((Number) result.get("correctBlocks")).intValue());
        assertTrue(result.get("issues") instanceof java.util.List);
    }
}
