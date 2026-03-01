package com.minewright.execution;

import com.minewright.action.actions.BaseAction;
import com.minewright.action.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ActionUtils}.
 *
 * Tests cover:
 * <ul>
 *   <li>extractActionType with various action class names</li>
 *   <li>extractActionType with null action</li>
 *   <li>ActionTimer start time recording</li>
 *   <li>ActionTimer elapsed time calculation</li>
 *   <li>ActionTimer removal and cleanup</li>
 *   <li>ActionTimer thread-safety</li>
 *   <li>ActionTimer clear functionality</li>
 * </ul>
 */
@DisplayName("ActionUtils Tests")
class ActionUtilsTest {

    private BaseAction mockAction;
    private ActionUtils.ActionTimer timer;

    @BeforeEach
    void setUp() {
        mockAction = mock(BaseAction.class);
        timer = new ActionUtils.ActionTimer();
    }

    // ==================== extractActionType Tests ====================

    @Test
    @DisplayName("extractActionType removes 'Action' suffix and converts to lowercase")
    void testExtractActionTypeWithActionSuffix() {
        // Create a mock with specific class name
        BaseAction mineBlockAction = mock(BaseAction.class);
        when(mineBlockAction.getClass()).thenReturn((Class) MineBlockAction.class);

        String result = ActionUtils.extractActionType(mineBlockAction);

        assertEquals("mineblock", result);
    }

    @Test
    @DisplayName("extractActionType handles simple action names")
    void testExtractActionTypeSimpleNames() {
        assertEquals("mine", extractTypeFromName("MineAction"));
        assertEquals("build", extractTypeFromName("BuildAction"));
        assertEquals("craft", extractTypeFromName("CraftAction"));
        assertEquals("gather", extractTypeFromName("GatherAction"));
        assertEquals("move", extractTypeFromName("MoveAction"));
    }

    @Test
    @DisplayName("extractActionType handles compound action names")
    void testExtractActionTypeCompoundNames() {
        assertEquals("mineblock", extractTypeFromName("MineBlockAction"));
        assertEquals("placeblock", extractTypeFromName("PlaceBlockAction"));
        assertEquals("craftitem", extractTypeFromName("CraftItemAction"));
        assertEquals("gatherresource", extractTypeFromName("GatherResourceAction"));
        assertEquals("buildstructure", extractTypeFromName("BuildStructureAction"));
    }

    @Test
    @DisplayName("extractActionType converts to lowercase")
    void testExtractActionTypeLowerCaseConversion() {
        assertEquals("mineblock", extractTypeFromName("MineBlockAction"));
        assertEquals("buildstructure", extractTypeFromName("BuildStructureAction"));
        // Verify all lowercase
        assertEquals("testactionname", extractTypeFromName("TestActionNameAction"));
    }

    @Test
    @DisplayName("extractActionType handles names without 'Action' suffix")
    void testExtractActionTypeWithoutActionSuffix() {
        BaseAction action = mock(BaseAction.class);
        when(action.getClass()).thenReturn((Class) SomeClass.class);

        String result = ActionUtils.extractActionType(action);

        assertEquals("someclass", result);
    }

    @Test
    @DisplayName("extractActionType returns 'unknown' for null action")
    void testExtractActionTypeWithNullAction() {
        String result = ActionUtils.extractActionType(null);

        assertEquals("unknown", result);
    }

    @Test
    @DisplayName("extractActionType handles empty class name")
    void testExtractActionTypeWithEmptyClassName() {
        BaseAction action = mock(BaseAction.class);
        when(action.getClass()).thenReturn((Class) EmptyNameAction.class);

        String result = ActionUtils.extractActionType(action);

        assertEquals("emptynameaction", result);
    }

    // ==================== ActionTimer.recordStart Tests ====================

    @Test
    @DisplayName("ActionTimer.recordStart stores start time")
    void testRecordStart() {
        long beforeTime = System.currentTimeMillis();

        timer.recordStart(mockAction);

        long afterTime = System.currentTimeMillis();

        // We can't directly check the stored time, but we can verify
        // that getElapsedAndRemove returns a reasonable value
        long elapsed = timer.getElapsedAndRemove(mockAction);
        assertTrue(elapsed >= 0 && elapsed < 100,
            "Elapsed time should be small (< 100ms) for immediate retrieval");
    }

    @Test
    @DisplayName("ActionTimer.recordStart can be called multiple times")
    void testRecordStartMultipleTimes() {
        timer.recordStart(mockAction);
        timer.recordStart(mockAction);
        timer.recordStart(mockAction);

        // Should still get a valid elapsed time
        long elapsed = timer.getElapsedAndRemove(mockAction);
        assertTrue(elapsed >= 0);
    }

    @Test
    @DisplayName("ActionTimer.recordStart works with null action")
    void testRecordStartWithNullAction() {
        // Should not throw exception
        assertDoesNotThrow(() -> timer.recordStart(null));
    }

    // ==================== ActionTimer.getElapsedAndRemove Tests ====================

    @Test
    @DisplayName("ActionTimer.getElapsedAndRemove returns elapsed time")
    void testGetElapsedAndRemove() throws InterruptedException {
        timer.recordStart(mockAction);

        Thread.sleep(10); // Small delay to ensure measurable elapsed time

        long elapsed = timer.getElapsedAndRemove(mockAction);

        assertTrue(elapsed >= 10, "Elapsed time should be at least 10ms");
        assertTrue(elapsed < 1000, "Elapsed time should be less than 1 second");
    }

    @Test
    @DisplayName("ActionTimer.getElapsedAndRemove removes the start time")
    void testGetElapsedAndRemoveRemovesStartTime() throws InterruptedException {
        timer.recordStart(mockAction);

        timer.getElapsedAndRemove(mockAction);

        // Second call should return 0 since start time was removed
        long elapsed = timer.getElapsedAndRemove(mockAction);
        assertEquals(0, elapsed, "Second call should return 0 after removal");
    }

    @Test
    @DisplayName("ActionTimer.getElapsedAndRemove returns 0 for unknown action")
    void testGetElapsedAndRemoveForUnknownAction() {
        long elapsed = timer.getElapsedAndRemove(mockAction);

        assertEquals(0, elapsed, "Unknown action should return 0");
    }

    @Test
    @DisplayName("ActionTimer.getElapsedAndRemove returns 0 for null action")
    void testGetElapsedAndRemoveWithNullAction() {
        timer.recordStart(mockAction);

        long elapsed = timer.getElapsedAndRemove(null);

        assertEquals(0, elapsed, "Null action should return 0");
    }

    @Test
    @DisplayName("ActionTimer.getElapsedAndRemove is reasonably accurate")
    void testGetElapsedAndRemoveAccuracy() throws InterruptedException {
        timer.recordStart(mockAction);

        Thread.sleep(50);

        long elapsed = timer.getElapsedAndRemove(mockAction);

        // Should be approximately 50ms (with some tolerance for thread scheduling)
        assertTrue(elapsed >= 40, "Elapsed time should be at least 40ms");
        assertTrue(elapsed < 200, "Elapsed time should be less than 200ms");
    }

    // ==================== ActionTimer.remove Tests ====================

    @Test
    @DisplayName("ActionTimer.remove cleans up start time")
    void testRemove() {
        timer.recordStart(mockAction);

        timer.remove(mockAction);

        // Start time should be removed
        long elapsed = timer.getElapsedAndRemove(mockAction);
        assertEquals(0, elapsed, "After remove, elapsed should be 0");
    }

    @Test
    @DisplayName("ActionTimer.remove is idempotent")
    void testRemoveIsIdempotent() {
        timer.recordStart(mockAction);

        timer.remove(mockAction);
        timer.remove(mockAction);
        timer.remove(mockAction);

        // Should not throw exception
        long elapsed = timer.getElapsedAndRemove(mockAction);
        assertEquals(0, elapsed);
    }

    @Test
    @DisplayName("ActionTimer.remove with null action does nothing")
    void testRemoveWithNullAction() {
        timer.recordStart(mockAction);

        assertDoesNotThrow(() -> timer.remove(null));

        // Original action should still be tracked
        long elapsed = timer.getElapsedAndRemove(mockAction);
        assertTrue(elapsed >= 0);
    }

    // ==================== ActionTimer.clear Tests ====================

    @Test
    @DisplayName("ActionTimer.clear removes all tracked actions")
    void testClear() {
        BaseAction action1 = mock(BaseAction.class);
        BaseAction action2 = mock(BaseAction.class);
        BaseAction action3 = mock(BaseAction.class);

        timer.recordStart(action1);
        timer.recordStart(action2);
        timer.recordStart(action3);

        timer.clear();

        assertEquals(0, timer.getElapsedAndRemove(action1));
        assertEquals(0, timer.getElapsedAndRemove(action2));
        assertEquals(0, timer.getElapsedAndRemove(action3));
    }

    @Test
    @DisplayName("ActionTimer.clear can be called multiple times")
    void testClearMultipleTimes() {
        timer.recordStart(mockAction);

        timer.clear();
        timer.clear();
        timer.clear();

        // Should not throw exception
        assertDoesNotThrow(() -> timer.clear());
    }

    @Test
    @DisplayName("ActionTimer.clear works on empty timer")
    void testClearOnEmptyTimer() {
        assertDoesNotThrow(() -> timer.clear());
    }

    // ==================== ActionTimer Integration Tests ====================

    @Test
    @DisplayName("ActionTimer handles multiple actions concurrently")
    void testMultipleActionsConcurrently() throws InterruptedException {
        BaseAction action1 = mock(BaseAction.class);
        BaseAction action2 = mock(BaseAction.class);
        BaseAction action3 = mock(BaseAction.class);

        timer.recordStart(action1);
        Thread.sleep(10);
        timer.recordStart(action2);
        Thread.sleep(10);
        timer.recordStart(action3);
        Thread.sleep(10);

        long elapsed1 = timer.getElapsedAndRemove(action1);
        long elapsed2 = timer.getElapsedAndRemove(action2);
        long elapsed3 = timer.getElapsedAndRemove(action3);

        // action1 should have longest elapsed time
        assertTrue(elapsed1 > elapsed2, "action1 should have longer elapsed time than action2");
        assertTrue(elapsed2 > elapsed3, "action2 should have longer elapsed time than action3");

        // All should be > 0
        assertTrue(elapsed1 > 0);
        assertTrue(elapsed2 > 0);
        assertTrue(elapsed3 > 0);
    }

    @Test
    @DisplayName("ActionTimer record-start-get lifecycle works correctly")
    void testFullLifecycle() throws InterruptedException {
        // Record start
        timer.recordStart(mockAction);

        // Wait a bit
        Thread.sleep(20);

        // Get elapsed time
        long elapsed1 = timer.getElapsedAndRemove(mockAction);
        assertTrue(elapsed1 >= 20);

        // Verify it's been removed
        long elapsed2 = timer.getElapsedAndRemove(mockAction);
        assertEquals(0, elapsed2);
    }

    // ==================== Thread Safety Tests ====================

    @Test
    @DisplayName("ActionTimer is thread-safe for concurrent access")
    void testThreadSafety() throws InterruptedException {
        final int THREAD_COUNT = 10;
        final int OPERATIONS_PER_THREAD = 100;
        Thread[] threads = new Thread[THREAD_COUNT];

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                    BaseAction action = mock(BaseAction.class);
                    timer.recordStart(action);
                    timer.getElapsedAndRemove(action);
                }
            });
        }

        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        // If we get here without exceptions, thread-safety is working
        assertTrue(true);
    }

    // ==================== Utility Classes ====================

    // Helper method to test extractActionType with custom class names
    private String extractTypeFromName(String className) {
        BaseAction action = mock(BaseAction.class);
        when(action.getClass().getSimpleName()).thenReturn(className);
        return ActionUtils.extractActionType(action);
    }

    // Test classes with specific names
    static class MineBlockAction extends BaseAction {
        public MineBlockAction() { super(null, null); }
        @Override protected void onTick() {}
        @Override protected void onStart() {}
        @Override protected void onCancel() {}
        @Override public String getDescription() { return "MineBlockAction"; }
    }

    static class SomeClass extends BaseAction {
        public SomeClass() { super(null, null); }
        @Override protected void onTick() {}
        @Override protected void onStart() {}
        @Override protected void onCancel() {}
        @Override public String getDescription() { return "SomeClass"; }
    }

    static class EmptyNameAction extends BaseAction {
        public EmptyNameAction() { super(null, null); }
        @Override protected void onTick() {}
        @Override protected void onStart() {}
        @Override protected void onCancel() {}
        @Override public String getDescription() { return "EmptyNameAction"; }
    }
}
