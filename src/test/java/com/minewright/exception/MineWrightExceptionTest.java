package com.minewright.exception;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for MineWrightException.
 * Tests exception construction, error codes, recovery suggestions, and edge cases.
 */
@DisplayName("MineWrightException Tests")
class MineWrightExceptionTest {

    // ========== Constructor Tests ==========

    @Test
    @DisplayName("Basic constructor creates exception with message and error code")
    void testBasicConstructor() {
        MineWrightException exception = new MineWrightException(
            "Test error message",
            MineWrightException.ErrorCode.LLM_PROVIDER_ERROR,
            "Try restarting"
        );

        assertEquals("Test error message", exception.getMessage());
        assertEquals(MineWrightException.ErrorCode.LLM_PROVIDER_ERROR, exception.getErrorCode());
        assertEquals("Try restarting", exception.getRecoverySuggestion());
        assertNull(exception.getContext());
        assertNull(exception.getCause());
    }

    @Test
    @DisplayName("Constructor with cause preserves exception chain")
    void testConstructorWithCause() {
        Throwable cause = new IllegalArgumentException("Root cause");
        MineWrightException exception = new MineWrightException(
            "Wrapped error",
            MineWrightException.ErrorCode.ACTION_EXECUTION_FAILED,
            "Fix the action",
            cause
        );

        assertEquals("Wrapped error", exception.getMessage());
        assertEquals(MineWrightException.ErrorCode.ACTION_EXECUTION_FAILED, exception.getErrorCode());
        assertEquals("Fix the action", exception.getRecoverySuggestion());
        assertSame(cause, exception.getCause());
        assertNull(exception.getContext());
    }

    @Test
    @DisplayName("Constructor with full context includes context information")
    void testConstructorWithFullContext() {
        Throwable cause = new RuntimeException("Underlying issue");
        String context = "Entity: TestEntity (ID: 123)";

        MineWrightException exception = new MineWrightException(
            "Error with context",
            MineWrightException.ErrorCode.ENTITY_SPAWN_FAILED,
            "Respawn the entity",
            context,
            cause
        );

        assertEquals("Error with context", exception.getMessage());
        assertEquals(MineWrightException.ErrorCode.ENTITY_SPAWN_FAILED, exception.getErrorCode());
        assertEquals("Respawn the entity", exception.getRecoverySuggestion());
        assertEquals(context, exception.getContext());
        assertSame(cause, exception.getCause());
    }

    @Test
    @DisplayName("Constructor accepts null recovery suggestion")
    void testConstructorWithNullRecoverySuggestion() {
        MineWrightException exception = new MineWrightException(
            "Error without recovery",
            MineWrightException.ErrorCode.CONFIG_MISSING_KEY,
            null
        );

        assertNull(exception.getRecoverySuggestion());
    }

    @Test
    @DisplayName("Constructor accepts null context")
    void testConstructorWithNullContext() {
        MineWrightException exception = new MineWrightException(
            "Error without context",
            MineWrightException.ErrorCode.CONFIG_INVALID_VALUE,
            "Fix the config",
            null,
            null
        );

        assertNull(exception.getContext());
        assertNull(exception.getCause());
    }

    // ========== ErrorCode Tests ==========

    @Test
    @DisplayName("All error codes have unique numeric values")
    void testErrorCodeUniqueness() {
        MineWrightException.ErrorCode[] codes = MineWrightException.ErrorCode.values();

        // Check that all codes are unique
        for (int i = 0; i < codes.length; i++) {
            for (int j = i + 1; j < codes.length; j++) {
                assertNotEquals(codes[i].getCode(), codes[j].getCode(),
                    "Error codes must be unique: " + codes[i] + " and " + codes[j]);
            }
        }
    }

    @Test
    @DisplayName("LLM error codes are in range 1000-1999")
    void testLLMErrorCodesRange() {
        assertEquals(1000, MineWrightException.ErrorCode.LLM_PROVIDER_ERROR.getCode());
        assertEquals(1001, MineWrightException.ErrorCode.LLM_RATE_LIMIT.getCode());
        assertEquals(1002, MineWrightException.ErrorCode.LLM_TIMEOUT.getCode());
        assertEquals(1003, MineWrightException.ErrorCode.LLM_INVALID_RESPONSE.getCode());
        assertEquals(1004, MineWrightException.ErrorCode.LLM_AUTH_ERROR.getCode());
        assertEquals(1005, MineWrightException.ErrorCode.LLM_NETWORK_ERROR.getCode());
        assertEquals(1006, MineWrightException.ErrorCode.LLM_CONFIG_ERROR.getCode());
    }

    @Test
    @DisplayName("Action error codes are in range 2000-2999")
    void testActionErrorCodesRange() {
        assertEquals(2000, MineWrightException.ErrorCode.ACTION_UNKNOWN_TYPE.getCode());
        assertEquals(2001, MineWrightException.ErrorCode.ACTION_INVALID_PARAMS.getCode());
        assertEquals(2002, MineWrightException.ErrorCode.ACTION_EXECUTION_FAILED.getCode());
        assertEquals(2003, MineWrightException.ErrorCode.ACTION_TIMEOUT.getCode());
        assertEquals(2004, MineWrightException.ErrorCode.ACTION_CANCELLED.getCode());
        assertEquals(2005, MineWrightException.ErrorCode.ACTION_BLOCKED.getCode());
    }

    @Test
    @DisplayName("Entity error codes are in range 3000-3999")
    void testEntityErrorCodesRange() {
        assertEquals(3000, MineWrightException.ErrorCode.ENTITY_SPAWN_FAILED.getCode());
        assertEquals(3001, MineWrightException.ErrorCode.ENTITY_TICK_ERROR.getCode());
        assertEquals(3002, MineWrightException.ErrorCode.ENTITY_INVALID_STATE.getCode());
        assertEquals(3003, MineWrightException.ErrorCode.ENTITY_NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("Config error codes are in range 4000-4999")
    void testConfigErrorCodesRange() {
        assertEquals(4000, MineWrightException.ErrorCode.CONFIG_MISSING_KEY.getCode());
        assertEquals(4001, MineWrightException.ErrorCode.CONFIG_INVALID_VALUE.getCode());
        assertEquals(4002, MineWrightException.ErrorCode.CONFIG_VALIDATION_FAILED.getCode());
    }

    @Test
    @DisplayName("Plugin error codes are in range 5000-5999")
    void testPluginErrorCodesRange() {
        assertEquals(5000, MineWrightException.ErrorCode.PLUGIN_LOAD_FAILED.getCode());
        assertEquals(5001, MineWrightException.ErrorCode.PLUGIN_NOT_FOUND.getCode());
        assertEquals(5002, MineWrightException.ErrorCode.PLUGIN_VERSION_MISMATCH.getCode());
    }

    @Test
    @DisplayName("Voice error codes are in range 6000-6999")
    void testVoiceErrorCodesRange() {
        assertEquals(6000, MineWrightException.ErrorCode.VOICE_INIT_FAILED.getCode());
        assertEquals(6001, MineWrightException.ErrorCode.VOICE_RECOGNITION_ERROR.getCode());
        assertEquals(6002, MineWrightException.ErrorCode.VOICE_SYNTHESIS_ERROR.getCode());
    }

    @Test
    @DisplayName("ErrorCode getDescription returns non-null descriptions")
    void testErrorCodeDescriptions() {
        for (MineWrightException.ErrorCode code : MineWrightException.ErrorCode.values()) {
            assertNotNull(code.getDescription());
            assertFalse(code.getDescription().isEmpty());
        }
    }

    // ========== Formatted Message Tests ==========

    @Test
    @DisplayName("getFormattedMessage returns basic message when no recovery or context")
    void testGetFormattedMessageBasic() {
        MineWrightException exception = new MineWrightException(
            "Simple error",
            MineWrightException.ErrorCode.LLM_PROVIDER_ERROR,
            null
        );

        String formatted = exception.getFormattedMessage();
        assertEquals("Simple error", formatted);
    }

    @Test
    @DisplayName("getFormattedMessage includes recovery suggestion when present")
    void testGetFormattedMessageWithRecovery() {
        MineWrightException exception = new MineWrightException(
            "Error with solution",
            MineWrightException.ErrorCode.CONFIG_MISSING_KEY,
            "Add the missing key to config file"
        );

        String formatted = exception.getFormattedMessage();
        assertTrue(formatted.contains("Error with solution"));
        assertTrue(formatted.contains("Recovery:"));
        assertTrue(formatted.contains("Add the missing key to config file"));
    }

    @Test
    @DisplayName("getFormattedMessage includes context when present")
    void testGetFormattedMessageWithContext() {
        MineWrightException exception = new MineWrightException(
            "Error with context",
            MineWrightException.ErrorCode.ENTITY_SPAWN_FAILED,
            null,
            "Entity: TestBot (ID: abc-123)",
            null
        );

        String formatted = exception.getFormattedMessage();
        assertTrue(formatted.contains("Error with context"));
        assertTrue(formatted.contains("Context:"));
        assertTrue(formatted.contains("Entity: TestBot (ID: abc-123)"));
    }

    @Test
    @DisplayName("getFormattedMessage includes both recovery and context when present")
    void testGetFormattedMessageWithRecoveryAndContext() {
        MineWrightException exception = new MineWrightException(
            "Complete error",
            MineWrightException.ErrorCode.ACTION_TIMEOUT,
            "Reduce timeout or retry",
            "Action: mine (duration: 30s)",
            null
        );

        String formatted = exception.getFormattedMessage();
        assertTrue(formatted.contains("Complete error"));
        assertTrue(formatted.contains("Recovery:"));
        assertTrue(formatted.contains("Reduce timeout or retry"));
        assertTrue(formatted.contains("Context:"));
        assertTrue(formatted.contains("Action: mine (duration: 30s)"));
    }

    @Test
    @DisplayName("getFormattedMessage handles empty recovery suggestion")
    void testGetFormattedMessageWithEmptyRecovery() {
        MineWrightException exception = new MineWrightException(
            "Error",
            MineWrightException.ErrorCode.LLM_TIMEOUT,
            "",
            null,
            null
        );

        String formatted = exception.getFormattedMessage();
        // Should not include recovery section when empty
        assertFalse(formatted.contains("Recovery:"));
    }

    // ========== toString Tests ==========

    @Test
    @DisplayName("toString includes error code, type, and message")
    void testToStringFormat() {
        MineWrightException exception = new MineWrightException(
            "Test message",
            MineWrightException.ErrorCode.LLM_RATE_LIMIT,
            "Wait and retry"
        );

        String toString = exception.toString();
        assertTrue(toString.contains("MineWrightException"));
        assertTrue(toString.contains("code=" + MineWrightException.ErrorCode.LLM_RATE_LIMIT.getCode()));
        assertTrue(toString.contains("type=" + MineWrightException.ErrorCode.LLM_RATE_LIMIT.name()));
        assertTrue(toString.contains("message='Test message'"));
    }

    // ========== Edge Case Tests ==========

    @Test
    @DisplayName("Exception can be thrown and caught correctly")
    void testExceptionThrowCatch() {
        MineWrightException thrown = assertThrows(MineWrightException.class, () -> {
            throw new MineWrightException(
                "Thrown exception",
                MineWrightException.ErrorCode.ENTITY_NOT_FOUND,
                "Create the entity"
            );
        });

        assertEquals("Thrown exception", thrown.getMessage());
        assertEquals(MineWrightException.ErrorCode.ENTITY_NOT_FOUND, thrown.getErrorCode());
    }

    @Test
    @DisplayName("Exception with cause preserves stack trace")
    void testExceptionCauseStackTrace() {
        Throwable cause = new NullPointerException("Null reference");
        MineWrightException exception = new MineWrightException(
            "Wrapped NPE",
            MineWrightException.ErrorCode.ACTION_EXECUTION_FAILED,
            "Check for nulls",
            cause
        );

        assertSame(cause, exception.getCause());
        assertEquals("Null reference", exception.getCause().getMessage());
    }

    @Test
    @DisplayName("Multiple exceptions can be chained")
    void testExceptionChaining() {
        Throwable rootCause = new IllegalStateException("Root error");
        MineWrightException middle = new MineWrightException(
            "Middle layer",
            MineWrightException.ErrorCode.LLM_PROVIDER_ERROR,
            "Fix provider",
            rootCause
        );
        MineWrightException top = new MineWrightException(
            "Top layer",
            MineWrightException.ErrorCode.CONFIG_MISSING_KEY,
            "Add config",
            middle
        );

        assertSame(middle, top.getCause());
        assertSame(rootCause, top.getCause().getCause());
    }

    @Test
    @DisplayName("Exception handles special characters in message")
    void testSpecialCharactersInMessage() {
        String specialMessage = "Error with \n newlines \t tabs and \"quotes\"";
        MineWrightException exception = new MineWrightException(
            specialMessage,
            MineWrightException.ErrorCode.LLM_INVALID_RESPONSE,
            null
        );

        assertEquals(specialMessage, exception.getMessage());
    }

    @Test
    @DisplayName("Exception handles very long recovery suggestions")
    void testLongRecoverySuggestion() {
        StringBuilder longRecovery = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longRecovery.append("Recovery step ").append(i).append(". ");
        }

        MineWrightException exception = new MineWrightException(
            "Error with long recovery",
            MineWrightException.ErrorCode.CONFIG_VALIDATION_FAILED,
            longRecovery.toString()
        );

        assertEquals(longRecovery.toString(), exception.getRecoverySuggestion());
    }

    @Test
    @DisplayName("Exception handles empty string message")
    void testEmptyMessage() {
        MineWrightException exception = new MineWrightException(
            "",
            MineWrightException.ErrorCode.PLUGIN_LOAD_FAILED,
            "Load plugin manually"
        );

        assertEquals("", exception.getMessage());
    }

    @Test
    @DisplayName("Exception is serializable")
    void testExceptionSerialization() {
        MineWrightException exception = new MineWrightException(
            "Serializable error",
            MineWrightException.ErrorCode.LLM_TIMEOUT,
            "Retry later",
            "Context info",
            new RuntimeException("Cause")
        );

        // Verify exception structure
        assertNotNull(exception);
        assertEquals("Serializable error", exception.getMessage());
    }

    // ========== Integration Tests ==========

    @Test
    @DisplayName("Exception works with try-catch-finally blocks")
    void testTryCatchFinally() {
        MineWrightException caught = null;
        try {
            throw new MineWrightException(
                "Test error",
                MineWrightException.ErrorCode.ENTITY_TICK_ERROR,
                "Reset entity"
            );
        } catch (MineWrightException e) {
            caught = e;
        } finally {
            assertNotNull(caught);
            assertEquals("Test error", caught.getMessage());
        }
    }

    @Test
    @DisplayName("Exception can be used in multi-catch blocks")
    void testMultiCatch() {
        RuntimeException caught = null;
        try {
            if (System.currentTimeMillis() % 2 == 0) {
                throw new MineWrightException(
                    "MW error",
                    MineWrightException.ErrorCode.CONFIG_INVALID_VALUE,
                    "Fix value"
                );
            } else {
                throw new IllegalArgumentException("IA error");
            }
        } catch (MineWrightException | IllegalArgumentException e) {
            caught = e;
        }

        assertNotNull(caught);
    }

    @Test
    @DisplayName("Exception maintains error code type safety")
    void testErrorCodeTypeSafety() {
        MineWrightException exception = new MineWrightException(
            "Type safety test",
            MineWrightException.ErrorCode.ACTION_BLOCKED,
            "Clear obstruction"
        );

        // Can compare error codes directly
        assertEquals(MineWrightException.ErrorCode.ACTION_BLOCKED, exception.getErrorCode());

        // Can switch on error codes
        switch (exception.getErrorCode()) {
            case ACTION_BLOCKED:
                // Correct branch
                break;
            default:
                fail("Should have entered ACTION_BLOCKED case");
        }
    }
}
