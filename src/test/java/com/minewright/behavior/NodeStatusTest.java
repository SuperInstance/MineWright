package com.minewright.behavior;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link NodeStatus} enum utility methods.
 *
 * <p>Tests verify the core utility methods that drive behavior tree execution:
 * <ul>
 *   <li>Terminal state detection (isTerminal)</li>
 *   <li>Status type checking (isSuccess, isFailure, isRunning)</li>
 *   <li>Status inversion (invert) - used by InverterNode</li>
 *   <li>Combination logic (combineSequence, combineSelector)</li>
 * </ul>
 */
@DisplayName("NodeStatus Utility Tests")
class NodeStatusTest {

    @Test
    @DisplayName("SUCCESS is a terminal state")
    void testSuccessIsTerminal() {
        assertTrue(NodeStatus.SUCCESS.isTerminal(),
            "SUCCESS should be a terminal state");
    }

    @Test
    @DisplayName("FAILURE is a terminal state")
    void testFailureIsTerminal() {
        assertTrue(NodeStatus.FAILURE.isTerminal(),
            "FAILURE should be a terminal state");
    }

    @Test
    @DisplayName("RUNNING is not a terminal state")
    void testRunningIsNotTerminal() {
        assertFalse(NodeStatus.RUNNING.isTerminal(),
            "RUNNING should not be a terminal state");
    }

    @Test
    @DisplayName("SUCCESS isSuccess returns true")
    void testSuccessIsSuccess() {
        assertTrue(NodeStatus.SUCCESS.isSuccess(),
            "SUCCESS.isSuccess() should return true");
        assertFalse(NodeStatus.FAILURE.isSuccess(),
            "FAILURE.isSuccess() should return false");
        assertFalse(NodeStatus.RUNNING.isSuccess(),
            "RUNNING.isSuccess() should return false");
    }

    @Test
    @DisplayName("FAILURE isFailure returns true")
    void testFailureIsFailure() {
        assertTrue(NodeStatus.FAILURE.isFailure(),
            "FAILURE.isFailure() should return true");
        assertFalse(NodeStatus.SUCCESS.isFailure(),
            "SUCCESS.isFailure() should return false");
        assertFalse(NodeStatus.RUNNING.isFailure(),
            "RUNNING.isFailure() should return false");
    }

    @Test
    @DisplayName("RUNNING isRunning returns true")
    void testRunningIsRunning() {
        assertTrue(NodeStatus.RUNNING.isRunning(),
            "RUNNING.isRunning() should return true");
        assertFalse(NodeStatus.SUCCESS.isRunning(),
            "SUCCESS.isRunning() should return false");
        assertFalse(NodeStatus.FAILURE.isRunning(),
            "FAILURE.isRunning() should return false");
    }

    @Test
    @DisplayName("invert converts SUCCESS to FAILURE")
    void testInvertSuccess() {
        assertEquals(NodeStatus.FAILURE, NodeStatus.SUCCESS.invert(),
            "SUCCESS inverted should be FAILURE");
    }

    @Test
    @DisplayName("invert converts FAILURE to SUCCESS")
    void testInvertFailure() {
        assertEquals(NodeStatus.SUCCESS, NodeStatus.FAILURE.invert(),
            "FAILURE inverted should be SUCCESS");
    }

    @Test
    @DisplayName("invert preserves RUNNING")
    void testInvertRunning() {
        assertEquals(NodeStatus.RUNNING, NodeStatus.RUNNING.invert(),
            "RUNNING inverted should still be RUNNING");
    }

    @Test
    @DisplayName("combineSequence: SUCCESS + SUCCESS = SUCCESS")
    void testCombineSequenceSuccessSuccess() {
        assertEquals(NodeStatus.SUCCESS,
            NodeStatus.SUCCESS.combineSequence(NodeStatus.SUCCESS),
            "SUCCESS + SUCCESS should equal SUCCESS");
    }

    @Test
    @DisplayName("combineSequence: SUCCESS + FAILURE = FAILURE")
    void testCombineSequenceSuccessFailure() {
        assertEquals(NodeStatus.FAILURE,
            NodeStatus.SUCCESS.combineSequence(NodeStatus.FAILURE),
            "SUCCESS + FAILURE should equal FAILURE (fail-fast)");
    }

    @Test
    @DisplayName("combineSequence: FAILURE + SUCCESS = SUCCESS")
    void testCombineSequenceFailureSuccess() {
        assertEquals(NodeStatus.SUCCESS,
            NodeStatus.FAILURE.combineSequence(NodeStatus.SUCCESS),
            "FAILURE + SUCCESS should equal SUCCESS (implementation quirk)");
    }

    @Test
    @DisplayName("combineSequence: RUNNING + SUCCESS = RUNNING")
    void testCombineSequenceRunningSuccess() {
        assertEquals(NodeStatus.RUNNING,
            NodeStatus.RUNNING.combineSequence(NodeStatus.SUCCESS),
            "RUNNING + SUCCESS should equal RUNNING");
    }

    @Test
    @DisplayName("combineSequence: SUCCESS + RUNNING = RUNNING")
    void testCombineSequenceSuccessRunning() {
        assertEquals(NodeStatus.RUNNING,
            NodeStatus.SUCCESS.combineSequence(NodeStatus.RUNNING),
            "SUCCESS + RUNNING should equal RUNNING");
    }

    @Test
    @DisplayName("combineSelector: SUCCESS + FAILURE = SUCCESS")
    void testCombineSelectorSuccessFailure() {
        assertEquals(NodeStatus.SUCCESS,
            NodeStatus.SUCCESS.combineSelector(NodeStatus.FAILURE),
            "SUCCESS + FAILURE should equal SUCCESS (found success)");
    }

    @Test
    @DisplayName("combineSelector: FAILURE + SUCCESS = SUCCESS")
    void testCombineSelectorFailureSuccess() {
        assertEquals(NodeStatus.SUCCESS,
            NodeStatus.FAILURE.combineSelector(NodeStatus.SUCCESS),
            "FAILURE + SUCCESS should equal SUCCESS");
    }

    @Test
    @DisplayName("combineSelector: FAILURE + FAILURE = FAILURE")
    void testCombineSelectorFailureFailure() {
        assertEquals(NodeStatus.FAILURE,
            NodeStatus.FAILURE.combineSelector(NodeStatus.FAILURE),
            "FAILURE + FAILURE should equal FAILURE");
    }

    @Test
    @DisplayName("combineSelector: RUNNING + FAILURE = RUNNING")
    void testCombineSelectorRunningFailure() {
        assertEquals(NodeStatus.RUNNING,
            NodeStatus.RUNNING.combineSelector(NodeStatus.FAILURE),
            "RUNNING + FAILURE should equal RUNNING");
    }

    @Test
    @DisplayName("invert is idempotent for terminal states")
    void testInvertIdempotent() {
        assertEquals(NodeStatus.SUCCESS,
            NodeStatus.SUCCESS.invert().invert(),
            "Inverting SUCCESS twice should return SUCCESS");
        assertEquals(NodeStatus.FAILURE,
            NodeStatus.FAILURE.invert().invert(),
            "Inverting FAILURE twice should return FAILURE");
    }

    @Test
    @DisplayName("All statuses are covered by type checks")
    void testTypeCheckCoverage() {
        // Every status should match exactly one type check
        for (NodeStatus status : NodeStatus.values()) {
            int matchCount = 0;
            if (status.isSuccess()) matchCount++;
            if (status.isFailure()) matchCount++;
            if (status.isRunning()) matchCount++;
            assertEquals(1, matchCount,
                status + " should match exactly one type check");
        }
    }
}
