package com.minewright.vision;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for VisionResponseParser.
 * Tests parsing of vision model API responses according to OpenAI-compatible format.
 */
@DisplayName("VisionResponseParser Tests")
class VisionResponseParserTest {

    private static final String VALID_RESPONSE = """
        {
            "id": "chatcmpl-123",
            "object": "chat.completion",
            "created": 1677652288,
            "model": "HuggingFaceTB/SmolVLM-Instruct",
            "choices": [
                {
                    "index": 0,
                    "message": {
                        "role": "assistant",
                        "content": "This is a plains biome with oak trees and a village visible in the distance."
                    },
                    "finish_reason": "stop"
                }
            ],
            "usage": {
                "prompt_tokens": 850,
                "completion_tokens": 150,
                "total_tokens": 1000
            }
        }
        """;

    private static final String ERROR_RESPONSE = """
        {
            "error": {
                "type": "invalid_request_error",
                "message": "Invalid image format"
            }
        }
        """;

    private static final String TRUNCATED_RESPONSE = """
        {
            "choices": [
                {
                    "message": {
                        "content": "Truncated response"
                    },
                    "finish_reason": "length"
                }
            ]
        }
        """;

    private static final String EMPTY_CHOICES_RESPONSE = """
        {
            "choices": []
        }
        """;

    private static final String MISSING_CONTENT_RESPONSE = """
        {
            "choices": [
                {
                    "message": {
                        "role": "assistant"
                    }
                }
            ]
        }
        """;

    private static final String INVALID_JSON = "not valid json";

    @BeforeEach
    void setUp() {
        // Setup if needed
    }

    // ==================== Content Extraction Tests ====================

    @Test
    @DisplayName("Should extract content from valid response")
    void testExtractContent_ValidResponse() {
        String content = VisionResponseParser.extractContent(VALID_RESPONSE);

        assertNotNull(content, "Content should not be null");
        assertEquals(
            "This is a plains biome with oak trees and a village visible in the distance.",
            content,
            "Content should match expected value"
        );
    }

    @Test
    @DisplayName("Should return null for empty choices array")
    void testExtractContent_EmptyChoices() {
        String content = VisionResponseParser.extractContent(EMPTY_CHOICES_RESPONSE);

        assertNull(content, "Content should be null for empty choices");
    }

    @Test
    @DisplayName("Should return null when message content is missing")
    void testExtractContent_MissingContent() {
        String content = VisionResponseParser.extractContent(MISSING_CONTENT_RESPONSE);

        assertNull(content, "Content should be null when content field is missing");
    }

    @Test
    @DisplayName("Should throw exception for invalid JSON")
    void testExtractContent_InvalidJson() {
        assertThrows(
            RuntimeException.class,
            () -> VisionResponseParser.extractContent(INVALID_JSON),
            "Should throw RuntimeException for invalid JSON"
        );
    }

    @Test
    @DisplayName("Should throw exception with API error details")
    void testExtractContent_ApiError() {
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> VisionResponseParser.extractContent(ERROR_RESPONSE),
            "Should throw RuntimeException for API error"
        );

        assertTrue(
            exception.getMessage().contains("API Error"),
            "Exception message should indicate API error"
        );
        assertTrue(
            exception.getMessage().contains("Invalid image format"),
            "Exception message should include error details"
        );
    }

    // ==================== Usage Statistics Tests ====================

    @Test
    @DisplayName("Should extract usage statistics from response")
    void testExtractUsageStats_ValidResponse() {
        VisionResponseParser.UsageStats stats = VisionResponseParser.extractUsageStats(VALID_RESPONSE);

        assertNotNull(stats, "Usage stats should not be null");
        assertEquals(850, stats.promptTokens(), "Prompt tokens should match");
        assertEquals(150, stats.completionTokens(), "Completion tokens should match");
        assertEquals(1000, stats.totalTokens(), "Total tokens should match");
    }

    @Test
    @DisplayName("Should return null for response without usage field")
    void testExtractUsageStats_MissingUsage() {
        String responseNoUsage = """
            {
                "choices": [
                    {
                        "message": {
                            "content": "Response"
                        }
                    }
                ]
            }
            """;

        VisionResponseParser.UsageStats stats = VisionResponseParser.extractUsageStats(responseNoUsage);

        assertNull(stats, "Usage stats should be null when usage field is missing");
    }

    @Test
    @DisplayName("Should return null for invalid response")
    void testExtractUsageStats_InvalidJson() {
        VisionResponseParser.UsageStats stats = VisionResponseParser.extractUsageStats(INVALID_JSON);

        assertNull(stats, "Usage stats should be null for invalid JSON");
    }

    // ==================== Truncation Detection Tests ====================

    @Test
    @DisplayName("Should detect truncated response")
    void testWasTruncated_TruncatedResponse() {
        boolean truncated = VisionResponseParser.wasTruncated(TRUNCATED_RESPONSE);

        assertTrue(truncated, "Should detect truncated response");
    }

    @Test
    @DisplayName("Should return false for complete response")
    void testWasTruncated_CompleteResponse() {
        boolean truncated = VisionResponseParser.wasTruncated(VALID_RESPONSE);

        assertFalse(truncated, "Should return false for complete response");
    }

    @Test
    @DisplayName("Should return false for malformed response")
    void testWasTruncated_MalformedResponse() {
        boolean truncated = VisionResponseParser.wasTruncated(INVALID_JSON);

        assertFalse(truncated, "Should return false for malformed response");
    }

    @Test
    @DisplayName("Should return false for empty choices")
    void testWasTruncated_EmptyChoices() {
        boolean truncated = VisionResponseParser.wasTruncated(EMPTY_CHOICES_RESPONSE);

        assertFalse(truncated, "Should return false for empty choices");
    }

    // ==================== Edge Case Tests ====================

    @Test
    @DisplayName("Should handle response with multiple choices")
    void testExtractContent_MultipleChoices() {
        String multiChoiceResponse = """
            {
                "choices": [
                    {
                        "message": {"content": "First choice"}
                    },
                    {
                        "message": {"content": "Second choice"}
                    }
                ]
            }
            """;

        String content = VisionResponseParser.extractContent(multiChoiceResponse);

        assertEquals("First choice", content, "Should extract first choice");
    }

    @Test
    @DisplayName("Should handle response with empty content string")
    void testExtractContent_EmptyString() {
        String emptyContentResponse = """
            {
                "choices": [
                    {
                        "message": {"content": ""}
                    }
                ]
            }
            """;

        String content = VisionResponseParser.extractContent(emptyContentResponse);

        assertEquals("", content, "Should handle empty string content");
    }

    @Test
    @DisplayName("Should handle response with nested JSON content")
    void testExtractContent_NestedJsonContent() {
        String nestedContentResponse = """
            {
                "choices": [
                    {
                        "message": {
                            "content": "{\\"biome\\": \\"plains\\", \\"resources\\": \\"oak trees\\"}"
                        }
                    }
                ]
            }
            """;

        String content = VisionResponseParser.extractContent(nestedContentResponse);

        assertTrue(
            content.contains("plains"),
            "Should extract nested JSON content as string"
        );
    }

    @Test
    @DisplayName("Should handle response with special characters in content")
    void testExtractContent_SpecialCharacters() {
        String specialCharResponse = """
            {
                "choices": [
                    {
                        "message": {
                            "content": "Biome: plains\\nResources: oak, birch\\nCoordinates: x=100, y=64, z=200"
                        }
                    }
                ]
            }
            """;

        String content = VisionResponseParser.extractContent(specialCharResponse);

        assertTrue(content.contains("\\n"), "Should preserve special characters");
    }

    @Test
    @DisplayName("Should handle response with very long content")
    void testExtractContent_LongContent() {
        StringBuilder longContent = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longContent.append("This is line ").append(i).append(" of the analysis. ");
        }

        String longContentResponse = String.format("""
            {
                "choices": [
                    {
                        "message": {"content": "%s"}
                    }
                ]
            }
            """, JsonParser.parseString("\"" + longContent + "\"").getAsString());

        String content = VisionResponseParser.extractContent(longContentResponse);

        assertTrue(content.length() > 10000, "Should handle long content");
    }

    // ==================== Error Classification Tests ====================

    @Test
    @DisplayName("Should classify invalid_request_error")
    void testClassifyError_InvalidRequest() {
        try {
            VisionResponseParser.extractContent(ERROR_RESPONSE);
            fail("Should have thrown exception");
        } catch (RuntimeException e) {
            assertTrue(
                e.getMessage().contains("Invalid image format"),
                "Should include error message"
            );
        }
    }

    @Test
    @DisplayName("Should handle authentication_error")
    void testClassifyError_AuthError() {
        String authErrorResponse = """
            {
                "error": {
                    "type": "authentication_error",
                    "message": "Invalid API key"
                }
            }
            """;

        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> VisionResponseParser.extractContent(authErrorResponse)
        );

        assertTrue(
            exception.getMessage().contains("Invalid API key"),
            "Should include authentication error message"
        );
    }

    @Test
    @DisplayName("Should handle rate_limit_error")
    void testClassifyError_RateLimit() {
        String rateLimitResponse = """
            {
                "error": {
                    "type": "rate_limit_error",
                    "message": "Too many requests"
                }
            }
            """;

        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> VisionResponseParser.extractContent(rateLimitResponse)
        );

        assertTrue(
            exception.getMessage().contains("Too many requests"),
            "Should include rate limit error message"
        );
    }

    @Test
    @DisplayName("Should handle server_error")
    void testClassifyError_ServerError() {
        String serverErrorResponse = """
            {
                "error": {
                    "type": "server_error",
                    "message": "Internal server error"
                }
            }
            """;

        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> VisionResponseParser.extractContent(serverErrorResponse)
        );

        assertTrue(
            exception.getMessage().contains("Internal server error"),
            "Should include server error message"
        );
    }

    // ==================== Null and Empty Input Tests ====================

    @Test
    @DisplayName("Should throw exception for null input")
    void testExtractContent_NullInput() {
        assertThrows(
            RuntimeException.class,
            () -> VisionResponseParser.extractContent(null),
            "Should throw exception for null input"
        );
    }

    @Test
    @DisplayName("Should throw exception for whitespace only")
    void testExtractContent_WhitespaceOnly() {
        assertThrows(
            RuntimeException.class,
            () -> VisionResponseParser.extractContent("   "),
            "Should throw exception for whitespace only"
        );
    }

    // ==================== Record Tests ====================

    @Test
    @DisplayName("UsageStats record should have correct values")
    void testUsageStatsRecord() {
        VisionResponseParser.UsageStats stats = new VisionResponseParser.UsageStats(100, 200, 300);

        assertEquals(100, stats.promptTokens(), "Prompt tokens should match");
        assertEquals(200, stats.completionTokens(), "Completion tokens should match");
        assertEquals(300, stats.totalTokens(), "Total tokens should match");
    }

    @Test
    @DisplayName("UsageStats record should implement equals correctly")
    void testUsageStatsEquals() {
        VisionResponseParser.UsageStats stats1 = new VisionResponseParser.UsageStats(100, 200, 300);
        VisionResponseParser.UsageStats stats2 = new VisionResponseParser.UsageStats(100, 200, 300);
        VisionResponseParser.UsageStats stats3 = new VisionResponseParser.UsageStats(100, 200, 299);

        assertEquals(stats1, stats2, "Equal stats should be equal");
        assertNotEquals(stats1, stats3, "Different stats should not be equal");
    }
}
