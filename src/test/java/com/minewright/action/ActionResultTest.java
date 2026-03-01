package com.minewright.action;

import com.minewright.exception.ActionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for {@link ActionResult}.
 *
 * Tests cover:
 * <ul>
 *   <li>Success result creation</li>
 *   <li>Failure result creation</li>
 *   <li>requiresReplanning flag behavior</li>
 *   <li>Factory methods</li>
 *   <li>toString output</li>
 *   <li>Error codes</li>
 *   <li>Timestamp and age tracking</li>
 *   <li>Recovery suggestions</li>
 *   <li>Exception handling</li>
 * </ul>
 */
@DisplayName("ActionResult Tests")
class ActionResultTest {

    @Test
    @DisplayName("Success result has correct flags")
    void testSuccessResult() {
        ActionResult result = ActionResult.success("Task completed successfully");

        assertTrue(result.isSuccess(), "Success result should return true for isSuccess()");
        assertEquals("Task completed successfully", result.getMessage(),
                "Message should match the input");
        assertFalse(result.requiresReplanning(),
                "Success result should not require replanning by default");
    }

    @Test
    @DisplayName("Failure result requires replanning by default")
    void testFailureResultRequiresReplanning() {
        ActionResult result = ActionResult.failure("Task failed");

        assertFalse(result.isSuccess(), "Failure result should return false for isSuccess()");
        assertEquals("Task failed", result.getMessage(), "Message should match the input");
        assertTrue(result.requiresReplanning(),
                "Failure result should require replanning by default");
    }

    @Test
    @DisplayName("Failure result can optionally not require replanning")
    void testFailureResultWithoutReplanning() {
        ActionResult result = ActionResult.failure("Minor error, continuing", false);

        assertFalse(result.isSuccess(), "Failure result should return false for isSuccess()");
        assertEquals("Minor error, continuing", result.getMessage());
        assertFalse(result.requiresReplanning(),
                "Failure result should respect requiresReplanning=false parameter");
    }

    @Test
    @DisplayName("Failure result can explicitly require replanning")
    void testFailureResultWithReplanning() {
        ActionResult result = ActionResult.failure("Critical error", true);

        assertFalse(result.isSuccess());
        assertEquals("Critical error", result.getMessage());
        assertTrue(result.requiresReplanning(),
                "Failure result should respect requiresReplanning=true parameter");
    }

    @Test
    @DisplayName("Constructor with all parameters")
    void testConstructorWithAllParameters() {
        ActionResult result = new ActionResult(true, "Custom message", true);

        assertTrue(result.isSuccess());
        assertEquals("Custom message", result.getMessage());
        assertTrue(result.requiresReplanning(),
                "Constructor should allow success with requiresReplanning=true");
    }

    @Test
    @DisplayName("Constructor defaults requiresReplanning to !success")
    void testConstructorDefaults() {
        ActionResult successResult = new ActionResult(true, "Success");
        assertTrue(successResult.isSuccess());
        assertFalse(successResult.requiresReplanning(),
                "Success should default requiresReplanning to false");

        ActionResult failureResult = new ActionResult(false, "Failure");
        assertFalse(failureResult.isSuccess());
        assertTrue(failureResult.requiresReplanning(),
                "Failure should default requiresReplanning to true");
    }

    @Test
    @DisplayName("Success factory method creates consistent results")
    void testSuccessFactoryMethodConsistency() {
        ActionResult result1 = ActionResult.success("Done");
        ActionResult result2 = ActionResult.success("Done");

        assertEquals(result1.isSuccess(), result2.isSuccess());
        assertEquals(result1.getMessage(), result2.getMessage());
        assertEquals(result1.requiresReplanning(), result2.requiresReplanning());
    }

    @Test
    @DisplayName("Failure factory method creates consistent results")
    void testFailureFactoryMethodConsistency() {
        ActionResult result1 = ActionResult.failure("Error");
        ActionResult result2 = ActionResult.failure("Error");

        assertEquals(result1.isSuccess(), result2.isSuccess());
        assertEquals(result1.getMessage(), result2.getMessage());
        assertEquals(result1.requiresReplanning(), result2.requiresReplanning());
    }

    @Test
    @DisplayName("ToString contains all relevant information")
    void testToString() {
        ActionResult successResult = ActionResult.success("Operation successful");
        String successString = successResult.toString();

        assertTrue(successString.contains("success=true"),
                "toString should contain success status");
        assertTrue(successString.contains("Operation successful"),
                "toString should contain the message");
        assertTrue(successString.contains("requiresReplanning=false"),
                "toString should contain replanning flag");

        ActionResult failureResult = ActionResult.failure("Operation failed");
        String failureString = failureResult.toString();

        assertTrue(failureString.contains("success=false"),
                "toString should contain success status for failure");
        assertTrue(failureString.contains("Operation failed"),
                "toString should contain the message for failure");
        assertTrue(failureString.contains("requiresReplanning=true"),
                "toString should contain replanning flag for failure");
    }

    @Test
    @DisplayName("Edge case: Empty message")
    void testEmptyMessage() {
        ActionResult result = new ActionResult(true, "");

        assertTrue(result.isSuccess());
        assertEquals("", result.getMessage());
    }

    @Test
    @DisplayName("Edge case: Null message handling")
    void testNullMessage() {
        ActionResult result = new ActionResult(true, null);

        assertTrue(result.isSuccess());
        assertNull(result.getMessage());
    }

    @Test
    @DisplayName("Edge case: Very long message")
    void testVeryLongMessage() {
        String longMessage = "A".repeat(10000);
        ActionResult result = ActionResult.success(longMessage);

        assertEquals(longMessage, result.getMessage(),
                "ActionResult should handle very long messages");
    }

    @Test
    @DisplayName("Special characters in message are preserved")
    void testSpecialCharactersInMessage() {
        String specialMessage = "Error: \n\t<>\"'{}[]&@#$%^*";
        ActionResult result = ActionResult.failure(specialMessage);

        assertEquals(specialMessage, result.getMessage(),
                "Special characters should be preserved in message");
    }

    @Test
    @DisplayName("Success with replanning true is possible via constructor")
    void testSuccessWithReplanningTrue() {
        // This might represent a scenario where we succeeded but need to replan
        // the next step differently
        ActionResult result = new ActionResult(true, "Completed but need new plan", true);

        assertTrue(result.isSuccess(),
                "Can be successful while requiring replanning");
        assertTrue(result.requiresReplanning(),
                "Success can still require replanning");
    }

    @Test
    @DisplayName("Multiple results can be compared")
    void testResultComparison() {
        ActionResult success1 = ActionResult.success("Done");
        ActionResult success2 = ActionResult.success("Done");
        ActionResult differentSuccess = ActionResult.success("Different");
        ActionResult failure = ActionResult.failure("Failed");

        // Results with same values should be equal
        assertEquals(success1.isSuccess(), success2.isSuccess());
        assertEquals(success1.getMessage(), success2.getMessage());

        // Results with different messages should differ
        assertNotEquals(success1.getMessage(), differentSuccess.getMessage());

        // Success and failure should differ
        assertNotEquals(success1.isSuccess(), failure.isSuccess());
    }

    @Test
    @DisplayName("Factory method overload without requiresReplanning parameter")
    void testFactoryMethodOverload() {
        // Test that the two-argument factory method works correctly
        ActionResult result1 = new ActionResult(false, "Error");
        ActionResult result2 = ActionResult.failure("Error");

        assertEquals(result1.isSuccess(), result2.isSuccess());
        assertEquals(result1.getMessage(), result2.getMessage());
        assertEquals(result1.requiresReplanning(), result2.requiresReplanning());
    }

    @Test
    @DisplayName("Result is immutable after creation")
    void testResultImmutability() {
        ActionResult result = ActionResult.success("Original message");

        String originalMessage = result.getMessage();
        boolean originalSuccess = result.isSuccess();
        boolean originalReplanning = result.requiresReplanning();

        // Since ActionResult is immutable with final fields,
        // we verify the values stay consistent
        assertEquals(originalMessage, result.getMessage());
        assertEquals(originalSuccess, result.isSuccess());
        assertEquals(originalReplanning, result.requiresReplanning());
    }

    // ==================== Error Code Tests ====================

    @Test
    @DisplayName("Success result has SUCCESS error code")
    void testSuccessErrorCode() {
        ActionResult result = ActionResult.success("Done");

        assertEquals(ActionResult.ErrorCode.SUCCESS, result.getErrorCode(),
            "Success result should have SUCCESS error code");
    }

    @Test
    @DisplayName("Failure result has UNKNOWN error code by default")
    void testFailureErrorCodeDefault() {
        ActionResult result = ActionResult.failure("Failed");

        assertEquals(ActionResult.ErrorCode.UNKNOWN, result.getErrorCode(),
            "Failure result should have UNKNOWN error code by default");
    }

    @Test
    @DisplayName("Failure result can have specific error code")
    void testFailureWithSpecificErrorCode() {
        ActionResult result = ActionResult.failure(
            ActionResult.ErrorCode.TIMEOUT,
            "Timed out",
            true
        );

        assertEquals(ActionResult.ErrorCode.TIMEOUT, result.getErrorCode());
    }

    @Test
    @DisplayName("All error codes have unique values")
    void testErrorCodesUnique() {
        ActionResult.ErrorCode[] codes = ActionResult.ErrorCode.values();

        // Check that each code has a unique numeric value
        for (int i = 0; i < codes.length; i++) {
            for (int j = i + 1; j < codes.length; j++) {
                assertNotEquals(codes[i].getCode(), codes[j].getCode(),
                    "Error codes should have unique values");
            }
        }
    }

    @Test
    @DisplayName("Error code has correct name")
    void testErrorCodeName() {
        ActionResult.ErrorCode timeout = ActionResult.ErrorCode.TIMEOUT;

        assertEquals("timeout", timeout.getName(),
            "Error code name should match expected value");
    }

    @Test
    @DisplayName("Error code has recovery category")
    void testErrorCodeRecoveryCategory() {
        ActionResult.ErrorCode timeout = ActionResult.ErrorCode.TIMEOUT;
        ActionResult.ErrorCode blocked = ActionResult.ErrorCode.BLOCKED;

        assertEquals(ErrorRecoveryStrategy.RecoveryCategory.TRANSIENT,
            timeout.getRecoveryCategory(),
            "TIMEOUT should be TRANSIENT");

        assertEquals(ErrorRecoveryStrategy.RecoveryCategory.RECOVERABLE,
            blocked.getRecoveryCategory(),
            "BLOCKED should be RECOVERABLE");
    }

    // ==================== Timestamp and Age Tests ====================

    @Test
    @DisplayName("Timestamp is set at creation")
    void testTimestampCreation() {
        long before = System.currentTimeMillis();
        ActionResult result = ActionResult.success("Test");
        long after = System.currentTimeMillis();

        assertTrue(result.getTimestamp() >= before &&
                   result.getTimestamp() <= after,
            "Timestamp should be between before and after");
    }

    @Test
    @DisplayName("Age increases over time")
    void testAgeIncreases() throws InterruptedException {
        ActionResult result = ActionResult.success("Test");

        long age1 = result.getAgeMs();
        Thread.sleep(10);
        long age2 = result.getAgeMs();

        assertTrue(age2 > age1,
            "Age should increase over time");
    }

    @Test
    @DisplayName("Age is zero immediately after creation")
    void testAgeZeroImmediately() {
        ActionResult result = ActionResult.success("Test");

        assertTrue(result.getAgeMs() >= 0 &&
                   result.getAgeMs() < 100,
            "Age should be very small immediately after creation");
    }

    // ==================== Recovery Suggestion Tests ====================

    @Test
    @DisplayName("Failure with recovery suggestion")
    void testFailureWithRecoverySuggestion() {
        String suggestion = "Try again with different parameters";
        ActionResult result = ActionResult.failureWithRecovery(
            "Operation failed",
            true,
            suggestion
        );

        assertEquals(suggestion, result.getRecoverySuggestion(),
            "Should store recovery suggestion");
    }

    @Test
    @DisplayName("Success has no recovery suggestion")
    void testSuccessNoRecoverySuggestion() {
        ActionResult result = ActionResult.success("Done");

        assertNull(result.getRecoverySuggestion(),
            "Success should not have recovery suggestion");
    }

    @Test
    @DisplayName("Failure without recovery suggestion returns null")
    void testFailureNoRecoverySuggestion() {
        ActionResult result = ActionResult.failure("Failed");

        assertNull(result.getRecoverySuggestion(),
            "Failure without suggestion should return null");
    }

    @Test
    @DisplayName("Formatted message includes recovery suggestion")
    void testFormattedMessageIncludesSuggestion() {
        String suggestion = "Check your network connection";
        ActionResult result = ActionResult.failureWithRecovery(
            "Network error",
            true,
            suggestion
        );

        String formatted = result.getFormattedMessage();
        assertTrue(formatted.contains("Network error"),
            "Should contain original message");
        assertTrue(formatted.contains(suggestion),
            "Should contain recovery suggestion");
    }

    @Test
    @DisplayName("Formatted message without suggestion returns original")
    void testFormattedMessageWithoutSuggestion() {
        ActionResult result = ActionResult.failure("Simple error");

        assertEquals("Simple error", result.getFormattedMessage(),
            "Should return original message when no suggestion");
    }

    // ==================== Timeout Factory Method Tests ====================

    @Test
    @DisplayName("Timeout factory method creates correct result")
    void testTimeoutFactoryMethod() {
        ActionResult result = ActionResult.timeout("mine_action", "30 seconds");

        assertFalse(result.isSuccess());
        assertEquals(ActionResult.ErrorCode.TIMEOUT, result.getErrorCode());
        assertTrue(result.requiresReplanning());
        assertTrue(result.getMessage().contains("mine_action"));
        assertTrue(result.getMessage().contains("30 seconds"));
    }

    @Test
    @DisplayName("Timeout has recovery suggestion")
    void testTimeoutHasRecoverySuggestion() {
        ActionResult result = ActionResult.timeout("build_action", "2 minutes");

        assertNotNull(result.getRecoverySuggestion(),
            "Timeout should have recovery suggestion");
        assertTrue(result.getRecoverySuggestion().toLowerCase().contains("try"),
            "Suggestion should mention trying again");
    }

    // ==================== Blocked Factory Method Tests ====================

    @Test
    @DisplayName("Blocked factory method creates correct result")
    void testBlockedFactoryMethod() {
        ActionResult result = ActionResult.blocked("move_action", "obstacle in path");

        assertFalse(result.isSuccess());
        assertEquals(ActionResult.ErrorCode.BLOCKED, result.getErrorCode());
        assertFalse(result.requiresReplanning(),
            "Blocked should not require replanning by default");
        assertTrue(result.getMessage().contains("move_action"));
        assertTrue(result.getMessage().contains("obstacle in path"));
    }

    @Test
    @DisplayName("Blocked has recovery suggestion")
    void testBlockedHasRecoverySuggestion() {
        ActionResult result = ActionResult.blocked("place_action", "position occupied");

        assertNotNull(result.getRecoverySuggestion());
        assertTrue(result.getRecoverySuggestion().toLowerCase().contains("clear") ||
                   result.getRecoverySuggestion().toLowerCase().contains("different"),
            "Suggestion should mention clearing or trying different approach");
    }

    // ==================== Exception Handling Tests ====================

    @Test
    @DisplayName("Failure from exception stores exception")
    void testFailureFromException() {
        ActionException exception = ActionException.executionFailed(
            "test_action",
            "Test failure",
            new RuntimeException("Cause")
        );

        ActionResult result = ActionResult.fromException(exception);

        assertFalse(result.isSuccess());
        assertNotNull(result.getException());
        assertEquals(exception, result.getException());
    }

    @Test
    @DisplayName("Failure from exception preserves message")
    void testFailureFromExceptionMessage() {
        ActionException exception = ActionException.executionFailed(
            "test_action",
            "Execution failed",
            null
        );

        ActionResult result = ActionResult.fromException(exception);

        assertTrue(result.getMessage().contains("Execution failed"));
    }

    @Test
    @DisplayName("Failure from exception with custom replanning")
    void testFailureFromExceptionReplanning() {
        ActionException exception = ActionException.executionFailed(
            "test_action",
            "Failed",
            null
        );

        ActionResult result1 = ActionResult.fromException(exception, true);
        ActionResult result2 = ActionResult.fromException(exception, false);

        assertTrue(result1.requiresReplanning());
        assertFalse(result2.requiresReplanning());
    }

    @Test
    @DisplayName("Failure from exception includes recovery suggestion")
    void testFailureFromExceptionRecovery() {
        ActionException exception = ActionException.invalidParameter(
            "test_action",
            "param",
            "Invalid parameter value"
        );

        ActionResult result = ActionResult.fromException(exception);

        assertNotNull(result.getRecoverySuggestion());
    }

    @Test
    @DisplayName("Failure from null exception uses UNKNOWN error code")
    void testFailureFromNullException() {
        // This tests edge case where exception might be null
        ActionResult result = ActionResult.fromException(null);

        assertEquals(ActionResult.ErrorCode.UNKNOWN, result.getErrorCode());
    }

    // ==================== Additional Edge Cases ====================

    @Test
    @DisplayName("Message with Unicode characters")
    void testUnicodeMessage() {
        String unicodeMessage = "Error: 测试 🚀 Ñoño";
        ActionResult result = ActionResult.failure(unicodeMessage);

        assertEquals(unicodeMessage, result.getMessage(),
            "Should handle Unicode characters");
    }

    @Test
    @DisplayName("Message with newlines and tabs")
    void testMessageWithWhitespace() {
        String message = "Line 1\nLine 2\tIndented";
        ActionResult result = ActionResult.failure(message);

        assertEquals(message, result.getMessage(),
            "Should preserve whitespace characters");
    }

    @Test
    @DisplayName("Multiple results created in quick succession have increasing timestamps")
    void testTimestampOrdering() {
        ActionResult result1 = ActionResult.success("First");
        ActionResult result2 = ActionResult.success("Second");
        ActionResult result3 = ActionResult.success("Third");

        assertTrue(result1.getTimestamp() <= result2.getTimestamp(),
            "Timestamps should be non-decreasing");
        assertTrue(result2.getTimestamp() <= result3.getTimestamp(),
            "Timestamps should be non-decreasing");
    }

    @Test
    @DisplayName("ToString includes error code")
    void testToStringIncludesErrorCode() {
        ActionResult result = ActionResult.failure(
            ActionResult.ErrorCode.TIMEOUT,
            "Timed out",
            true
        );

        String toString = result.toString();
        assertTrue(toString.contains("errorCode="),
            "toString should include error code");
        assertTrue(toString.contains("TIMEOUT") ||
                   toString.contains("timeout"),
            "toString should include error code name");
    }

    @Test
    @DisplayName("ToString includes timestamp")
    void testToStringIncludesTimestamp() {
        ActionResult result = ActionResult.success("Done");

        String toString = result.toString();
        assertTrue(toString.contains("timestamp="),
            "toString should include timestamp");
    }

    @Test
    @DisplayName("ToString includes exception presence")
    void testToStringIncludesException() {
        ActionException exception = ActionException.executionFailed(
            "test",
            "Failed",
            null
        );
        ActionResult result = ActionResult.fromException(exception);

        String toString = result.toString();
        assertTrue(toString.contains("hasException="),
            "toString should include exception flag");
        assertTrue(toString.contains("hasException=true"),
            "toString should show exception is present");
    }

    @Test
    @DisplayName("Success without replanning is most common pattern")
    void testSuccessNoReplanningPattern() {
        // This is the most common pattern - success with no replanning needed
        ActionResult result = ActionResult.success("Task completed");

        assertTrue(result.isSuccess());
        assertFalse(result.requiresReplanning());
        assertEquals("Task completed", result.getMessage());
        assertEquals(ActionResult.ErrorCode.SUCCESS, result.getErrorCode());
        assertNull(result.getRecoverySuggestion());
        assertNull(result.getException());
    }

    @Test
    @DisplayName("Critical failure requires replanning")
    void testCriticalFailurePattern() {
        ActionResult result = ActionResult.failure(
            ActionResult.ErrorCode.INVALID_PARAMS,
            "Invalid parameters provided",
            true
        );

        assertFalse(result.isSuccess());
        assertTrue(result.requiresReplanning());
        assertEquals(ActionResult.ErrorCode.INVALID_PARAMS, result.getErrorCode());
    }

    @Test
    @DisplayName("Transient failure may not require replanning")
    void testTransientFailurePattern() {
        ActionResult result = ActionResult.failure(
            ActionResult.ErrorCode.RESOURCE_UNAVAILABLE,
            "Resource temporarily unavailable",
            false
        );

        assertFalse(result.isSuccess());
        assertFalse(result.requiresReplanning(),
            "Transient failure might not need replanning");
        assertEquals(ActionResult.ErrorCode.RESOURCE_UNAVAILABLE, result.getErrorCode());
    }
}
