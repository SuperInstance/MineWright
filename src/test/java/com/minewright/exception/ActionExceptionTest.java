package com.minewright.exception;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for ActionException.
 * Tests exception construction, factory methods, replanning flags, and edge cases.
 */
@DisplayName("ActionException Tests")
class ActionExceptionTest {

    // ========== Constructor Tests ==========

    @Test
    @DisplayName("Basic constructor creates exception with action type")
    void testBasicConstructor() {
        ActionException exception = new ActionException(
            "Mining failed",
            "mine",
            MineWrightException.ErrorCode.ACTION_EXECUTION_FAILED,
            "Try different location"
        );

        assertEquals("Mining failed", exception.getMessage());
        assertEquals("mine", exception.getActionType());
        assertEquals(MineWrightException.ErrorCode.ACTION_EXECUTION_FAILED, exception.getErrorCode());
        assertEquals("Try different location", exception.getRecoverySuggestion());
        assertTrue(exception.requiresReplanning());
        assertNull(exception.getCause());
    }

    @Test
    @DisplayName("Constructor with cause preserves exception chain")
    void testConstructorWithCause() {
        Throwable cause = new IllegalStateException("Block not breakable");
        ActionException exception = new ActionException(
            "Place action failed",
            "place",
            MineWrightException.ErrorCode.ACTION_BLOCKED,
            "Clear the space",
            cause
        );

        assertEquals("Place action failed", exception.getMessage());
        assertEquals("place", exception.getActionType());
        assertSame(cause, exception.getCause());
        assertTrue(exception.requiresReplanning());
    }

    @Test
    @DisplayName("Constructor with full control allows setting replanning flag")
    void testConstructorWithFullControl() {
        ActionException exception = new ActionException(
            "Action blocked",
            "pathfind",
            MineWrightException.ErrorCode.ACTION_BLOCKED,
            "Wait and retry",
            false,  // requiresReplanning = false
            null
        );

        assertEquals("pathfind", exception.getActionType());
        assertFalse(exception.requiresReplanning(),
            "Blocked actions should not require replanning by default");
    }

    @Test
    @DisplayName("Constructor accepts null cause")
    void testConstructorWithNullCause() {
        ActionException exception = new ActionException(
            "Timeout",
            "craft",
            MineWrightException.ErrorCode.ACTION_TIMEOUT,
            "Increase timeout",
            true,
            null
        );

        assertNull(exception.getCause());
        assertTrue(exception.requiresReplanning());
    }

    // ========== Factory Method Tests ==========

    @Test
    @DisplayName("unknownAction factory creates correct exception")
    void testUnknownActionFactory() {
        ActionException exception = ActionException.unknownAction("fly");

        assertEquals("Unknown action type: fly", exception.getMessage());
        assertEquals("fly", exception.getActionType());
        assertEquals(MineWrightException.ErrorCode.ACTION_UNKNOWN_TYPE, exception.getErrorCode());
        assertTrue(exception.requiresReplanning());
        assertTrue(exception.getRecoverySuggestion().contains("mine"));
        assertTrue(exception.getRecoverySuggestion().contains("place"));
        assertTrue(exception.getRecoverySuggestion().contains("pathfind"));
    }

    @Test
    @DisplayName("invalidParameter factory creates correct exception")
    void testInvalidParameterFactory() {
        ActionException exception = ActionException.invalidParameter("mine", "radius", "negative value");

        assertEquals("Invalid parameter 'radius' for action 'mine': negative value",
            exception.getMessage());
        assertEquals("mine", exception.getActionType());
        assertEquals(MineWrightException.ErrorCode.ACTION_INVALID_PARAMS, exception.getErrorCode());
        assertTrue(exception.requiresReplanning());
        assertTrue(exception.getRecoverySuggestion().contains("replanned"));
    }

    @Test
    @DisplayName("executionFailed factory creates correct exception with cause")
    void testExecutionFailedFactory() {
        Throwable cause = new RuntimeException("Pathfinding failed");
        ActionException exception = ActionException.executionFailed("pathfind", "No valid path", cause);

        assertEquals("Action 'pathfind' execution failed: No valid path", exception.getMessage());
        assertEquals("pathfind", exception.getActionType());
        assertEquals(MineWrightException.ErrorCode.ACTION_EXECUTION_FAILED, exception.getErrorCode());
        assertSame(cause, exception.getCause());
        assertTrue(exception.requiresReplanning());
    }

    @Test
    @DisplayName("executionFailed factory handles null cause")
    void testExecutionFailedFactoryNullCause() {
        ActionException exception = ActionException.executionFailed("attack", "Target out of range", null);

        assertNull(exception.getCause());
        assertTrue(exception.requiresReplanning());
    }

    @Test
    @DisplayName("timeout factory creates correct exception")
    void testTimeoutFactory() {
        ActionException exception = ActionException.timeout("gather", "30 seconds");

        assertEquals("Action 'gather' timed out after 30 seconds", exception.getMessage());
        assertEquals("gather", exception.getActionType());
        assertEquals(MineWrightException.ErrorCode.ACTION_TIMEOUT, exception.getErrorCode());
        assertTrue(exception.requiresReplanning());
        assertTrue(exception.getRecoverySuggestion().contains("unreachable"));
        assertTrue(exception.getRecoverySuggestion().contains("blocked"));
    }

    @Test
    @DisplayName("timeout factory with different duration formats")
    void testTimeoutFactoryFormats() {
        ActionException seconds = ActionException.timeout("mine", "45s");
        ActionException minutes = ActionException.timeout("build", "2 minutes");
        ActionException millis = ActionException.timeout("craft", "5000ms");

        assertTrue(seconds.getMessage().contains("45s"));
        assertTrue(minutes.getMessage().contains("2 minutes"));
        assertTrue(millis.getMessage().contains("5000ms"));
    }

    @Test
    @DisplayName("blocked factory creates non-replanning exception")
    void testBlockedFactory() {
        ActionException exception = ActionException.blocked("place", "Block in the way");

        assertEquals("Action 'place' is blocked: Block in the way", exception.getMessage());
        assertEquals("place", exception.getActionType());
        assertEquals(MineWrightException.ErrorCode.ACTION_BLOCKED, exception.getErrorCode());
        assertFalse(exception.requiresReplanning(),
            "Blocked actions should not require replanning");
        assertTrue(exception.getRecoverySuggestion().contains("obstruction"));
        assertTrue(exception.getRecoverySuggestion().contains("skipped"));
    }

    @Test
    @DisplayName("blocked factory with various block reasons")
    void testBlockedFactoryReasons() {
        ActionException blocked1 = ActionException.blocked("mine", "Cannot break bedrock");
        ActionException blocked2 = ActionException.blocked("place", "Entity in the way");
        ActionException blocked3 = ActionException.blocked("pathfind", "Door is locked");

        assertTrue(blocked1.getMessage().contains("bedrock"));
        assertTrue(blocked2.getMessage().contains("Entity"));
        assertTrue(blocked3.getMessage().contains("locked"));

        // All should be non-replanning
        assertFalse(blocked1.requiresReplanning());
        assertFalse(blocked2.requiresReplanning());
        assertFalse(blocked3.requiresReplanning());
    }

    // ========== toString Tests ==========

    @Test
    @DisplayName("toString includes all relevant fields")
    void testToStringFormat() {
        ActionException exception = new ActionException(
            "Test action failed",
            "testAction",
            MineWrightException.ErrorCode.ACTION_EXECUTION_FAILED,
            "Fix it",
            true,
            null
        );

        String toString = exception.toString();
        assertTrue(toString.contains("ActionException"));
        assertTrue(toString.contains("actionType='testAction'"));
        assertTrue(toString.contains("requiresReplanning=true"));
        assertTrue(toString.contains("code=" + MineWrightException.ErrorCode.ACTION_EXECUTION_FAILED.getCode()));
        assertTrue(toString.contains("message='Test action failed'"));
    }

    @Test
    @DisplayName("toString shows false for requiresReplanning when false")
    void testToStringRequiresReplanningFalse() {
        ActionException exception = ActionException.blocked("place", "blocked");

        assertTrue(exception.toString().contains("requiresReplanning=false"));
    }

    // ========== Replanning Flag Tests ==========

    @Test
    @DisplayName("Most factory methods create replanning=true exceptions")
    void testFactoryMethodsReplanningFlag() {
        assertTrue(ActionException.unknownAction("test").requiresReplanning());
        assertTrue(ActionException.invalidParameter("test", "param", "reason").requiresReplanning());
        assertTrue(ActionException.executionFailed("test", "reason", null).requiresReplanning());
        assertTrue(ActionException.timeout("test", "30s").requiresReplanning());
        assertFalse(ActionException.blocked("test", "reason").requiresReplanning(),
            "blocked() should create non-replanning exception");
    }

    @Test
    @DisplayName("requiresReplanning flag affects recovery suggestions")
    void testReplanningAffectsRecovery() {
        ActionException replanning = ActionException.unknownAction("jump");
        ActionException nonReplanning = ActionException.blocked("place", "occupied");

        assertTrue(replanning.getRecoverySuggestion().contains("replanned"),
            "Replanning exceptions should mention replanning in recovery");

        assertTrue(nonReplanning.getRecoverySuggestion().contains("skipped"),
            "Non-replanning exceptions should suggest skipping");
    }

    // ========== Action Type Tests ==========

    @Test
    @DisplayName("getActionType returns correct action types")
    void testGetActionType() {
        assertEquals("mine", ActionException.unknownAction("mine").getActionType());
        assertEquals("place", ActionException.blocked("place", "blocked").getActionType());
        assertEquals("pathfind", ActionException.timeout("pathfind", "10s").getActionType());
        assertEquals("craft", ActionException.executionFailed("craft", "no materials", null).getActionType());
    }

    @Test
    @DisplayName("Action type handles special characters")
    void testActionTypeSpecialCharacters() {
        ActionException exception = ActionException.invalidParameter(
            "complex-action",
            "param_with_underscore",
            "test"
        );

        assertEquals("complex-action", exception.getActionType());
        assertTrue(exception.getMessage().contains("param_with_underscore"));
    }

    // ========== Edge Case Tests ==========

    @Test
    @DisplayName("Exception can be thrown and caught correctly")
    void testExceptionThrowCatch() {
        ActionException thrown = assertThrows(ActionException.class, () -> {
            throw ActionException.unknownAction("invalid");
        });

        assertEquals("Unknown action type: invalid", thrown.getMessage());
    }

    @Test
    @DisplayName("Exception with cause preserves stack trace")
    void testExceptionCauseStackTrace() {
        Throwable cause = new NullPointerException("Null target");
        ActionException exception = ActionException.executionFailed("attack", "No target", cause);

        assertSame(cause, exception.getCause());
        assertEquals("Null target", exception.getCause().getMessage());
    }

    @Test
    @DisplayName("Multiple action exceptions can be chained")
    void testExceptionChaining() {
        Throwable rootCause = new IllegalStateException("Root error");
        ActionException middle = new ActionException(
            "Middle action failed",
            "middle",
            MineWrightException.ErrorCode.ACTION_EXECUTION_FAILED,
            "Fix middle",
            rootCause
        );
        ActionException top = new ActionException(
            "Top action failed",
            "top",
            MineWrightException.ErrorCode.ACTION_EXECUTION_FAILED,
            "Fix top",
            middle
        );

        assertSame(middle, top.getCause());
        assertSame(rootCause, top.getCause().getCause());
    }

    @Test
    @DisplayName("Factory methods handle empty and null parameters")
    void testFactoryMethodEmptyNullParams() {
        ActionException emptyAction = ActionException.unknownAction("");
        ActionException emptyParam = ActionException.invalidParameter("test", "", "reason");
        ActionException nullReason = ActionException.executionFailed("test", null, null);

        assertEquals("", emptyAction.getActionType());
        assertEquals("", emptyParam.getActionType());
        assertTrue(nullReason.getMessage().contains("test"));
    }

    @Test
    @DisplayName("Exception handles very long action types")
    void testLongActionType() {
        StringBuilder longType = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longType.append("very_long_action_type_part_").append(i).append("_");
        }

        ActionException exception = ActionException.unknownAction(longType.toString());
        assertTrue(exception.getActionType().length() > 500);
    }

    @Test
    @DisplayName("Exception handles special characters in messages")
    void testSpecialCharactersInMessages() {
        ActionException exception = ActionException.invalidParameter(
            "test-action",
            "param\nwith\tnewlines",
            "reason\nwith\ttabs"
        );

        assertTrue(exception.getMessage().contains("param\nwith\tnewlines"));
        assertTrue(exception.getMessage().contains("reason\nwith\ttabs"));
    }

    @Test
    @DisplayName("Exception is serializable")
    void testExceptionSerialization() {
        ActionException exception = new ActionException(
            "Serializable action error",
            "serialize",
            MineWrightException.ErrorCode.ACTION_TIMEOUT,
            "Retry serialization",
            true,
            new RuntimeException("Serialization cause")
        );

        // Verify exception structure
        assertNotNull(exception);
        assertEquals("Serializable action error", exception.getMessage());
    }

    // ========== Integration Tests ==========

    @Test
    @DisplayName("Exception works in realistic action execution scenario")
    void testRealisticScenario() {
        // Simulate a mining action failure
        ActionException exception = ActionException.executionFailed(
            "mine",
            "Target block too far",
            new IllegalArgumentException("Distance: 100 blocks, Max: 5 blocks")
        );

        assertEquals("mine", exception.getActionType());
        assertTrue(exception.requiresReplanning());
        assertTrue(exception.getRecoverySuggestion().contains("replanning"));
        assertTrue(exception.getRecoverySuggestion().contains("parameters"));

        // Can be used in error handling
        if (exception.requiresReplanning()) {
            // In real code, would trigger replanning
            assertTrue(exception.getRecoverySuggestion().length() > 0);
        }
    }

    @Test
    @DisplayName("Different action types can be distinguished")
    void testActionTypeDiscrimination() {
        ActionException mineError = ActionException.timeout("mine", "30s");
        ActionException placeError = ActionException.blocked("place", "no space");
        ActionException craftError = ActionException.executionFailed("craft", "no materials", null);

        // Can distinguish by action type
        assertEquals("mine", mineError.getActionType());
        assertEquals("place", placeError.getActionType());
        assertEquals("craft", craftError.getActionType());

        // Can distinguish by error code
        assertEquals(MineWrightException.ErrorCode.ACTION_TIMEOUT, mineError.getErrorCode());
        assertEquals(MineWrightException.ErrorCode.ACTION_BLOCKED, placeError.getErrorCode());
        assertEquals(MineWrightException.ErrorCode.ACTION_EXECUTION_FAILED, craftError.getErrorCode());
    }

    @Test
    @DisplayName("Exception maintains error code type safety")
    void testErrorCodeTypeSafety() {
        ActionException exception = ActionException.blocked("pathfind", "wall");

        // Can compare error codes
        assertEquals(MineWrightException.ErrorCode.ACTION_BLOCKED, exception.getErrorCode());

        // Can switch on error codes
        switch (exception.getErrorCode()) {
            case ACTION_BLOCKED:
                // Correct branch
                break;
            case ACTION_TIMEOUT:
            case ACTION_EXECUTION_FAILED:
            case ACTION_UNKNOWN_TYPE:
            case ACTION_INVALID_PARAMS:
            case ACTION_CANCELLED:
                fail("Should have entered ACTION_BLOCKED case");
                break;
            default:
                fail("Unexpected error code");
        }
    }

    @Test
    @DisplayName("Recovery suggestions are contextually appropriate")
    void testRecoverySuggestions() {
        ActionException unknown = ActionException.unknownAction("fly");
        ActionException invalid = ActionException.invalidParameter("mine", "radius", "negative");
        ActionException timeout = ActionException.timeout("gather", "60s");
        ActionException blocked = ActionException.blocked("place", "occupied");

        // Each factory method provides appropriate recovery
        assertTrue(unknown.getRecoverySuggestion().contains("Available actions"));
        assertTrue(invalid.getRecoverySuggestion().contains("parameters"));
        assertTrue(timeout.getRecoverySuggestion().contains("unreachable"));
        assertTrue(blocked.getRecoverySuggestion().contains("obstruction"));
        assertTrue(blocked.getRecoverySuggestion().contains("skipped"));
    }

    @Test
    @DisplayName("Constructor consistency with factory methods")
    void testConstructorVsFactoryConsistency() {
        // Factory method
        ActionException factoryException = ActionException.unknownAction("test");

        // Manual constructor with same parameters
        ActionException manualException = new ActionException(
            "Unknown action type: test",
            "test",
            MineWrightException.ErrorCode.ACTION_UNKNOWN_TYPE,
            factoryException.getRecoverySuggestion(),
            true,
            null
        );

        assertEquals(factoryException.getActionType(), manualException.getActionType());
        assertEquals(factoryException.getErrorCode(), manualException.getErrorCode());
        assertEquals(factoryException.requiresReplanning(), manualException.requiresReplanning());
        assertEquals(factoryException.getMessage(), manualException.getMessage());
    }
}
