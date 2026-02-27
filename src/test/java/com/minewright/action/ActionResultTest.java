package com.minewright.action;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ActionResult}.
 *
 * Tests cover:
 * <ul>
 *   <li>Success result creation</li>
 *   <li>Failure result creation</li>
 *   <li>requiresReplanning flag behavior</li>
 *   <li>Factory methods</li>
 *   <li>toString output</li>
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
}
