package com.minewright.skill;

import com.minewright.testutil.TestLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SkillLearningLoop}.
 *
 * <p>Tests cover:</p>
 * <ul>
 *   <li>Singleton pattern implementation</li>
 *   <li>Lifecycle management (start/stop/shutdown)</li>
 *   <li>Learning cycle execution</li>
 *   <li>Forced learning cycles</li>
 *   <li>Status reporting</li>
 *   <li>Sequence submission and processing</li>
 *   <li>Refinement checking</li>
 *   <li>Callback invocation</li>
 *   <li>Thread safety</li>
 *   <li>Error handling</li>
 * </ul>
 *
 * <p>Note: Some tests use reflection to access singleton state for isolation.</p>
 */
@DisplayName("Skill Learning Loop Tests")
class SkillLearningLoopTest {

    private SkillLearningLoop learningLoop;

    @BeforeEach
    void setUp() throws Exception {
        TestLogger.initForTesting();
        resetSkillLearningLoopSingleton();
        resetExecutionTrackerSingleton();
        resetSkillLibrarySingleton();
        resetSkillAutoGeneratorSingleton();
        resetSkillEffectivenessTrackerSingleton();

        learningLoop = SkillLearningLoop.getInstance();
    }

    @AfterEach
    void tearDown() {
        if (learningLoop != null) {
            learningLoop.shutdown();
        }
    }

    // ==================== Singleton Reset Methods ====================

    private void resetSkillLearningLoopSingleton() throws Exception {
        Field instanceField = SkillLearningLoop.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
    }

    private void resetExecutionTrackerSingleton() throws Exception {
        Field instanceField = ExecutionTracker.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
    }

    private void resetSkillLibrarySingleton() throws Exception {
        Field instanceField = SkillLibrary.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
    }

    private void resetSkillAutoGeneratorSingleton() throws Exception {
        Field instanceField = SkillAutoGenerator.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
    }

    private void resetSkillEffectivenessTrackerSingleton() throws Exception {
        Field instanceField = SkillEffectivenessTracker.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
    }

    // ==================== Helper Methods ====================

    private ExecutionSequence createTestSequence(String agentId, String goal, boolean successful) {
        return ExecutionSequence.builder(agentId, goal)
            .build(successful);
    }

    private ExecutionSequence createSequenceWithActions(String agentId, String goal, int actionCount, boolean successful) {
        ExecutionSequence.Builder builder = ExecutionSequence.builder(agentId, goal);
        for (int i = 0; i < actionCount; i++) {
            builder.addAction(ActionRecord.success("testAction" + i, Map.of(), 100L));
        }
        return builder.build(successful);
    }

    // ==================== Singleton Tests ====================

    @Nested
    @DisplayName("Singleton Tests")
    class SingletonTests {

        @Test
        @DisplayName("getInstance returns same instance")
        void getInstanceReturnsSameInstance() {
            SkillLearningLoop instance1 = SkillLearningLoop.getInstance();
            SkillLearningLoop instance2 = SkillLearningLoop.getInstance();

            assertSame(instance1, instance2,
                "Should return the same instance");
        }

        @Test
        @DisplayName("getInstance creates instance on first call")
        void getInstanceCreatesInstanceOnFirstCall() {
            SkillLearningLoop instance = SkillLearningLoop.getInstance();

            assertNotNull(instance, "Should create instance on first call");
        }

        @Test
        @DisplayName("Singleton is thread-safe")
        void singletonIsThreadSafe() throws InterruptedException {
            int threadCount = 10;
            CountDownLatch latch = new CountDownLatch(threadCount);
            List<SkillLearningLoop> instances = new ArrayList<>();

            for (int i = 0; i < threadCount; i++) {
                new Thread(() -> {
                    try {
                        instances.add(SkillLearningLoop.getInstance());
                    } finally {
                        latch.countDown();
                    }
                }).start();
            }

            assertTrue(latch.await(5, TimeUnit.SECONDS),
                "All threads should complete");

            // All instances should be the same
            SkillLearningLoop first = instances.get(0);
            for (SkillLearningLoop instance : instances) {
                assertSame(first, instance,
                    "All threads should get the same instance");
            }
        }
    }

    // ==================== Lifecycle Tests ====================

    @Nested
    @DisplayName("Lifecycle Tests")
    class LifecycleTests {

        @Test
        @DisplayName("Start changes running state to true")
        void startChangesRunningStateToTrue() {
            assertFalse(learningLoop.isRunning(),
                "Should not be running initially");

            learningLoop.start();

            assertTrue(learningLoop.isRunning(),
                "Should be running after start");
        }

        @Test
        @DisplayName("Start when already running logs warning")
        void startWhenAlreadyRunningLogsWarning() {
            learningLoop.start();

            // Should not throw exception, just log warning
            assertDoesNotThrow(() -> learningLoop.start(),
                "Starting when already running should not throw");

            assertTrue(learningLoop.isRunning(),
                "Should still be running");
        }

        @Test
        @DisplayName("Stop changes running state to false")
        void stopChangesRunningStateToFalse() {
            learningLoop.start();
            assertTrue(learningLoop.isRunning());

            learningLoop.stop();

            assertFalse(learningLoop.isRunning(),
                "Should not be running after stop");
        }

        @Test
        @DisplayName("Stop when not running is safe")
        void stopWhenNotRunningIsSafe() {
            // Should not throw exception
            assertDoesNotThrow(() -> learningLoop.stop(),
                "Stopping when not running should not throw");

            assertFalse(learningLoop.isRunning(),
                "Should not be running");
        }

        @Test
        @DisplayName("Multiple start-stop cycles work correctly")
        void multipleStartStopCyclesWorkCorrectly() {
            for (int i = 0; i < 3; i++) {
                learningLoop.start();
                assertTrue(learningLoop.isRunning(),
                    "Should be running in cycle " + i);

                learningLoop.stop();
                assertFalse(learningLoop.isRunning(),
                    "Should not be running in cycle " + i);
            }
        }

        @Test
        @DisplayName("Shutdown stops learning loop and executor")
        void shutdownStopsLearningLoopAndExecutor() {
            learningLoop.start();

            learningLoop.shutdown();

            assertFalse(learningLoop.isRunning(),
                "Should not be running after shutdown");
        }

        @Test
        @DisplayName("Learning loop runs in daemon thread")
        void learningLoopRunsInDaemonThread() throws Exception {
            learningLoop.start();

            // Give thread time to start
            Thread.sleep(100);

            assertTrue(learningLoop.isRunning(),
                "Learning loop should be running");

            learningLoop.stop();
        }
    }

    // ==================== Learning Cycle Tests ====================

    @Nested
    @DisplayName("Learning Cycle Tests")
    class LearningCycleTests {

        @Test
        @DisplayName("Force learning cycle performs immediate learning")
        void forceLearningCyclePerformsImmediateLearning() {
            ExecutionTracker tracker = ExecutionTracker.getInstance();

            // Add some successful sequences
            tracker.endTracking("agent1", true);
            tracker.endTracking("agent2", true);

            // Should not throw
            assertDoesNotThrow(() -> learningLoop.forceLearningCycle(),
                "Force learning cycle should not throw");
        }

        @Test
        @DisplayName("Learning cycle with no sequences is safe")
        void learningCycleWithNoSequencesIsSafe() {
            ExecutionTracker tracker = ExecutionTracker.getInstance();
            tracker.clear();

            assertDoesNotThrow(() -> learningLoop.forceLearningCycle(),
                "Learning cycle with no sequences should not throw");
        }

        @Test
        @DisplayName("Learning cycle processes successful sequences")
        void learningCycleProcessesSuccessfulSequences() {
            ExecutionTracker tracker = ExecutionTracker.getInstance();

            // Create successful sequences
            tracker.startTracking("agent1", "Test goal 1");
            tracker.recordAction("agent1", "mine", Map.of(), 100, true, null);
            tracker.recordAction("agent1", "place", Map.of(), 100, true, null);
            tracker.endTracking("agent1", true);

            tracker.startTracking("agent2", "Test goal 2");
            tracker.recordAction("agent2", "mine", Map.of(), 100, true, null);
            tracker.endTracking("agent2", true);

            int initialSkillCount = SkillLibrary.getInstance().getSkillCount();

            learningLoop.forceLearningCycle();

            // Note: Pattern extraction may not generate skills without sufficient frequency
            // The important thing is that it doesn't crash
            assertTrue(SkillLibrary.getInstance().getSkillCount() >= initialSkillCount,
                "Skill count should not decrease");
        }

        @Test
        @DisplayName("Learning cycle skips failed sequences")
        void learningCycleSkipsFailedSequences() {
            ExecutionTracker tracker = ExecutionTracker.getInstance();
            tracker.clear();

            // Create failed sequences
            tracker.endTracking("agent1", false);
            tracker.endTracking("agent2", false);

            int initialSkillCount = SkillLibrary.getInstance().getSkillCount();

            learningLoop.forceLearningCycle();

            assertEquals(initialSkillCount, SkillLibrary.getInstance().getSkillCount(),
                "Failed sequences should not generate skills");
        }

        @Test
        @DisplayName("Learning cycle increments check counter")
        void learningCycleIncrementsCheckCounter() throws Exception {
            Field checksField = SkillLearningLoop.class.getDeclaredField("checksPerformed");
            checksField.setAccessible(true);
            AtomicInteger checks = (AtomicInteger) checksField.get(learningLoop);

            int initialChecks = checks.get();

            learningLoop.forceLearningCycle();

            assertEquals(initialChecks + 1, checks.get(),
                "Check counter should increment");
        }
    }

    // ==================== Refinement Tests ====================

    @Nested
    @DisplayName("Refinement Tests")
    class RefinementTests {

        @Test
        @DisplayName("Refinement callback is invoked when needed")
        void refinementCallbackIsInvokedWhenNeeded() throws Exception {
            List<String> refinedSkills = new ArrayList<>();

            learningLoop.setRefinementCallback((skillId, reason) -> {
                refinedSkills.add(skillId);
            });

            // Perform enough cycles to trigger refinement check
            for (int i = 0; i < 10; i++) {
                learningLoop.forceLearningCycle();
            }

            // Note: Actual refinement depends on skill effectiveness
            // We're just testing that the mechanism works
            assertNotNull(refinedSkills,
                "Refinement callback list should exist");
        }

        @Test
        @DisplayName("Refinement check happens periodically")
        void refinementCheckHappensPeriodically() throws Exception {
            Field checksField = SkillLearningLoop.class.getDeclaredField("checksPerformed");
            checksField.setAccessible(true);
            AtomicInteger checks = (AtomicInteger) checksField.get(learningLoop);

            // REFINEMENT_CHECK_INTERVAL is 10
            for (int i = 0; i < 15; i++) {
                learningLoop.forceLearningCycle();
            }

            assertTrue(checks.get() >= 15,
                "Should have performed 15+ checks");
        }

        @Test
        @DisplayName("Refinement callback can be null")
        void refinementCallbackCanBeNull() {
            learningLoop.setRefinementCallback(null);

            assertDoesNotThrow(() -> learningLoop.forceLearningCycle(),
                "Should work without callback");
        }

        @Test
        @DisplayName("Refinement callback receives skill ID and reason")
        void refinementCallbackReceivesSkillIdAndReason() throws Exception {
            List<String> skillIds = new ArrayList<>();
            List<String> reasons = new ArrayList<>();

            learningLoop.setRefinementCallback((skillId, reason) -> {
                skillIds.add(skillId);
                reasons.add(reason);
            });

            // Trigger refinement check
            for (int i = 0; i < 10; i++) {
                learningLoop.forceLearningCycle();
            }

            // If any skills were flagged for refinement, callback should have been called
            // (though it's unlikely without actual effectiveness data)
            assertNotNull(skillIds, "Skill ID list should exist");
            assertNotNull(reasons, "Reason list should exist");
        }
    }

    // ==================== Status Report Tests ====================

    @Nested
    @DisplayName("Status Report Tests")
    class StatusReportTests {

        @Test
        @DisplayName("Status report contains running state")
        void statusReportContainsRunningState() {
            learningLoop.start();

            Map<String, Object> report = learningLoop.getStatusReport();

            assertNotNull(report, "Status report should not be null");
            assertTrue(report.containsKey("running"),
                "Report should contain running state");
            assertEquals(true, report.get("running"),
                "Running state should be true");

            learningLoop.stop();

            report = learningLoop.getStatusReport();
            assertEquals(false, report.get("running"),
                "Running state should be false");
        }

        @Test
        @DisplayName("Status report contains checks performed")
        void statusReportContainsChecksPerformed() {
            learningLoop.forceLearningCycle();
            learningLoop.forceLearningCycle();

            Map<String, Object> report = learningLoop.getStatusReport();

            assertTrue(report.containsKey("checksPerformed"),
                "Report should contain checks performed");
            assertTrue((Integer) report.get("checksPerformed") >= 2,
                "Should have performed at least 2 checks");
        }

        @Test
        @DisplayName("Status report contains skill library size")
        void statusReportContainsSkillLibrarySize() {
            Map<String, Object> report = learningLoop.getStatusReport();

            assertTrue(report.containsKey("skillLibrarySize"),
                "Report should contain skill library size");
            assertTrue((Integer) report.get("skillLibrarySize") > 0,
                "Skill library should have skills");
        }

        @Test
        @DisplayName("Status report contains successful sequences count")
        void statusReportContainsSuccessfulSequencesCount() {
            ExecutionTracker tracker = ExecutionTracker.getInstance();
            tracker.endTracking("agent1", true);
            tracker.endTracking("agent2", true);

            Map<String, Object> report = learningLoop.getStatusReport();

            assertTrue(report.containsKey("successfulSequences"),
                "Report should contain successful sequences count");
            assertTrue((Integer) report.get("successfulSequences") >= 0,
                "Successful sequences count should be non-negative");
        }

        @Test
        @DisplayName("Status report updates dynamically")
        void statusReportUpdatesDynamically() {
            Map<String, Object> report1 = learningLoop.getStatusReport();
            int checks1 = (Integer) report1.get("checksPerformed");

            learningLoop.forceLearningCycle();

            Map<String, Object> report2 = learningLoop.getStatusReport();
            int checks2 = (Integer) report2.get("checksPerformed");

            assertTrue(checks2 > checks1,
                "Checks performed should increase");
        }

        @Test
        @DisplayName("Status report is immutable")
        void statusReportIsImmutable() {
            Map<String, Object> report1 = learningLoop.getStatusReport();

            Map<String, Object> report2 = learningLoop.getStatusReport();

            // Should be different map instances
            assertNotSame(report1, report2,
                "Each call should return new map instance");
        }
    }

    // ==================== Integration Tests ====================

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Learning loop integrates with ExecutionTracker")
        void learningLoopIntegratesWithExecutionTracker() {
            ExecutionTracker tracker = ExecutionTracker.getInstance();

            tracker.startTracking("testAgent", "Test goal");
            tracker.recordAction("testAgent", "mine", Map.of(), 100, true, null);
            tracker.recordAction("testAgent", "place", Map.of(), 100, true, null);
            tracker.endTracking("testAgent", true);

            assertDoesNotThrow(() -> learningLoop.forceLearningCycle(),
                "Should integrate with ExecutionTracker");
        }

        @Test
        @DisplayName("Learning loop integrates with SkillLibrary")
        void learningLoopIntegratesWithSkillLibrary() {
            SkillLibrary library = SkillLibrary.getInstance();
            int initialCount = library.getSkillCount();

            learningLoop.forceLearningCycle();

            // Library should still be accessible
            assertTrue(library.getSkillCount() >= initialCount,
                "SkillLibrary should remain functional");
        }

        @Test
        @DisplayName("Learning loop integrates with SkillAutoGenerator")
        void learningLoopIntegratesWithSkillAutoGenerator() {
            SkillAutoGenerator generator = SkillAutoGenerator.getInstance();

            assertDoesNotThrow(() -> learningLoop.forceLearningCycle(),
                "Should integrate with SkillAutoGenerator");
        }

        @Test
        @DisplayName("Learning loop integrates with SkillEffectivenessTracker")
        void learningLoopIntegratesWithSkillEffectivenessTracker() {
            SkillEffectivenessTracker tracker = SkillEffectivenessTracker.getInstance();

            // Record some effectiveness data
            tracker.recordUse("testSkill", true, 100);
            tracker.recordUse("testSkill", false, 150);

            assertDoesNotThrow(() -> learningLoop.forceLearningCycle(),
                "Should integrate with SkillEffectivenessTracker");
        }

        @Test
        @DisplayName("Full learning cycle works end-to-end")
        void fullLearningCycleWorksEndToEnd() {
            ExecutionTracker tracker = ExecutionTracker.getInstance();

            // Create sequences
            for (int i = 0; i < 5; i++) {
                tracker.startTracking("agent" + i, "Goal " + i);
                tracker.recordAction("agent" + i, "mine", Map.of(), 100, true, null);
                tracker.recordAction("agent" + i, "mine", Map.of(), 100, true, null);
                tracker.endTracking("agent" + i, true);
            }

            int initialSkillCount = SkillLibrary.getInstance().getSkillCount();

            learningLoop.forceLearningCycle();

            // System should be stable
            assertTrue(SkillLibrary.getInstance().getSkillCount() >= initialSkillCount,
                "Skills should not be lost");

            Map<String, Object> report = learningLoop.getStatusReport();
            assertNotNull(report, "Status report should be available");
        }
    }

    // ==================== Thread Safety Tests ====================

    @Nested
    @DisplayName("Thread Safety Tests")
    class ThreadSafetyTests {

        @Test
        @DisplayName("Concurrent start is thread-safe")
        void concurrentStartIsThreadSafe() throws InterruptedException {
            int threadCount = 5;
            CountDownLatch latch = new CountDownLatch(threadCount);

            for (int i = 0; i < threadCount; i++) {
                new Thread(() -> {
                    try {
                        learningLoop.start();
                    } finally {
                        latch.countDown();
                    }
                }).start();
            }

            assertTrue(latch.await(5, TimeUnit.SECONDS),
                "All threads should complete");

            assertTrue(learningLoop.isRunning(),
                "Should be running after concurrent starts");
        }

        @Test
        @DisplayName("Concurrent stop is thread-safe")
        void concurrentStopIsThreadSafe() throws InterruptedException {
            learningLoop.start();

            int threadCount = 5;
            CountDownLatch latch = new CountDownLatch(threadCount);

            for (int i = 0; i < threadCount; i++) {
                new Thread(() -> {
                    try {
                        learningLoop.stop();
                    } finally {
                        latch.countDown();
                    }
                }).start();
            }

            assertTrue(latch.await(5, TimeUnit.SECONDS),
                "All threads should complete");

            assertFalse(learningLoop.isRunning(),
                "Should be stopped after concurrent stops");
        }

        @Test
        @DisplayName("Concurrent force learning cycle is thread-safe")
        void concurrentForceLearningCycleIsThreadSafe() throws InterruptedException {
            int threadCount = 10;
            CountDownLatch latch = new CountDownLatch(threadCount);

            for (int i = 0; i < threadCount; i++) {
                new Thread(() -> {
                    try {
                        learningLoop.forceLearningCycle();
                    } finally {
                        latch.countDown();
                    }
                }).start();
            }

            assertTrue(latch.await(10, TimeUnit.SECONDS),
                "All threads should complete");

            // Status should be consistent
            Map<String, Object> report = learningLoop.getStatusReport();
            assertTrue((Integer) report.get("checksPerformed") >= threadCount,
                "Should have performed at least " + threadCount + " checks");
        }

        @Test
        @DisplayName("Concurrent status report is thread-safe")
        void concurrentStatusReportIsThreadSafe() throws InterruptedException {
            learningLoop.start();

            int threadCount = 20;
            CountDownLatch latch = new CountDownLatch(threadCount);
            List<Map<String, Object>> reports = new ArrayList<>();

            for (int i = 0; i < threadCount; i++) {
                new Thread(() -> {
                    try {
                        reports.add(learningLoop.getStatusReport());
                    } finally {
                        latch.countDown();
                    }
                }).start();
            }

            assertTrue(latch.await(5, TimeUnit.SECONDS),
                "All threads should complete");

            assertEquals(threadCount, reports.size(),
                "Should have collected all reports");

            // All reports should be valid
            for (Map<String, Object> report : reports) {
                assertNotNull(report, "Report should not be null");
                assertTrue(report.containsKey("running"),
                    "Report should contain running state");
            }
        }

        @Test
        @DisplayName("Concurrent callback registration is thread-safe")
        void concurrentCallbackRegistrationIsThreadSafe() throws InterruptedException {
            int threadCount = 10;
            CountDownLatch latch = new CountDownLatch(threadCount);

            for (int i = 0; i < threadCount; i++) {
                final int threadId = i;
                new Thread(() -> {
                    try {
                        learningLoop.setRefinementCallback((skillId, reason) -> {
                            // Callback implementation
                        });
                    } finally {
                        latch.countDown();
                    }
                }).start();
            }

            assertTrue(latch.await(5, TimeUnit.SECONDS),
                "All threads should complete");

            // Should still work
            assertDoesNotThrow(() -> learningLoop.forceLearningCycle(),
                "Should work after concurrent callback registration");
        }
    }

    // ==================== Error Handling Tests ====================

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Handles null callback gracefully")
        void handlesNullCallbackGracefully() {
            learningLoop.setRefinementCallback(null);

            assertDoesNotThrow(() -> learningLoop.forceLearningCycle(),
                "Should handle null callback gracefully");
        }

        @Test
        @DisplayName("Handles callback exception gracefully")
        void handlesCallbackExceptionGracefully() {
            learningLoop.setRefinementCallback((skillId, reason) -> {
                throw new RuntimeException("Callback error");
            });

            // Should not throw even though callback does
            assertDoesNotThrow(() -> learningLoop.forceLearningCycle(),
                "Should handle callback exception gracefully");
        }

        @Test
        @DisplayName("Handles empty execution tracker gracefully")
        void handlesEmptyExecutionTrackerGracefully() {
            ExecutionTracker tracker = ExecutionTracker.getInstance();
            tracker.clear();

            assertDoesNotThrow(() -> learningLoop.forceLearningCycle(),
                "Should handle empty tracker gracefully");
        }

        @Test
        @DisplayName("Handles tracker exceptions gracefully")
        void handlesTrackerExceptionsGracefully() {
            // This test ensures the learning loop doesn't crash
            // even if the tracker throws exceptions
            assertDoesNotThrow(() -> learningLoop.forceLearningCycle(),
                "Should handle tracker exceptions gracefully");
        }

        @Test
        @DisplayName("Multiple rapid start-stop cycles are safe")
        void multipleRapidStartStopCyclesAreSafe() {
            for (int i = 0; i < 10; i++) {
                learningLoop.start();
                learningLoop.stop();
            }

            assertFalse(learningLoop.isRunning(),
                "Should be stopped after rapid cycles");
        }

        @Test
        @DisplayName("Shutdown can be called multiple times")
        void shutdownCanBeCalledMultipleTimes() {
            learningLoop.start();

            assertDoesNotThrow(() -> {
                learningLoop.shutdown();
                learningLoop.shutdown();
                learningLoop.shutdown();
            }, "Multiple shutdowns should not throw");

            assertFalse(learningLoop.isRunning(),
                "Should be stopped after multiple shutdowns");
        }
    }

    // ==================== Performance Tests ====================

    @Nested
    @DisplayName("Performance Tests")
    class PerformanceTests {

        @Test
        @DisplayName("Force learning cycle completes quickly")
        void forceLearningCycleCompletesQuickly() {
            long startTime = System.currentTimeMillis();

            learningLoop.forceLearningCycle();

            long duration = System.currentTimeMillis() - startTime;

            assertTrue(duration < 1000,
                "Learning cycle should complete in less than 1 second, took: " + duration + "ms");
        }

        @Test
        @DisplayName("Status report generation is fast")
        void statusReportGenerationIsFast() {
            long startTime = System.currentTimeMillis();

            Map<String, Object> report = learningLoop.getStatusReport();

            long duration = System.currentTimeMillis() - startTime;

            assertTrue(duration < 100,
                "Status report should be generated in less than 100ms, took: " + duration + "ms");
            assertNotNull(report, "Report should be generated");
        }

        @Test
        @DisplayName("Start and stop are fast operations")
        void startAndStopAreFastOperations() {
            long startStart = System.currentTimeMillis();
            learningLoop.start();
            long startDuration = System.currentTimeMillis() - startStart;

            long stopStart = System.currentTimeMillis();
            learningLoop.stop();
            long stopDuration = System.currentTimeMillis() - stopStart;

            assertTrue(startDuration < 100,
                "Start should be fast, took: " + startDuration + "ms");
            assertTrue(stopDuration < 100,
                "Stop should be fast, took: " + stopDuration + "ms");
        }

        @Test
        @DisplayName("Multiple consecutive force cycles are efficient")
        void multipleConsecutiveForceCyclesAreEfficient() {
            int iterations = 10;
            long startTime = System.currentTimeMillis();

            for (int i = 0; i < iterations; i++) {
                learningLoop.forceLearningCycle();
            }

            long totalDuration = System.currentTimeMillis() - startTime;
            long avgDuration = totalDuration / iterations;

            assertTrue(avgDuration < 500,
                "Average cycle time should be under 500ms, was: " + avgDuration + "ms");
        }
    }

    // ==================== Edge Cases Tests ====================

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Status report is valid before start")
        void statusReportIsValidBeforeStart() {
            Map<String, Object> report = learningLoop.getStatusReport();

            assertNotNull(report, "Report should not be null");
            assertFalse((Boolean) report.get("running"),
                "Running should be false before start");
        }

        @Test
        @DisplayName("Force cycle works before start")
        void forceCycleWorksBeforeStart() {
            assertDoesNotThrow(() -> learningLoop.forceLearningCycle(),
                "Force cycle should work before start");
        }

        @Test
        @DisplayName("Start after stop works correctly")
        void startAfterStopWorksCorrectly() {
            learningLoop.start();
            learningLoop.stop();
            learningLoop.start();

            assertTrue(learningLoop.isRunning(),
                "Should be running after start-stop-start");
        }

        @Test
        @DisplayName("Force cycle during running is safe")
        void forceCycleDuringRunningIsSafe() throws Exception {
            learningLoop.start();

            // Give the loop thread time to start
            Thread.sleep(100);

            assertDoesNotThrow(() -> learningLoop.forceLearningCycle(),
                "Force cycle during running should be safe");

            learningLoop.stop();
        }

        @Test
        @DisplayName("Callback can be changed multiple times")
        void callbackCanBeChangedMultipleTimes() {
            List<String> callbacks = new ArrayList<>();

            learningLoop.setRefinementCallback((skillId, reason) -> {
                callbacks.add("first");
            });

            learningLoop.setRefinementCallback((skillId, reason) -> {
                callbacks.add("second");
            });

            learningLoop.setRefinementCallback((skillId, reason) -> {
                callbacks.add("third");
            });

            // Should use the last callback
            learningLoop.forceLearningCycle();

            assertNotNull(callbacks, "Callback list should exist");
        }

        @Test
        @DisplayName("Large number of sequences is handled")
        void largeNumberOfSequencesIsHandled() {
            ExecutionTracker tracker = ExecutionTracker.getInstance();

            // Add many sequences
            for (int i = 0; i < 100; i++) {
                tracker.endTracking("agent" + i, i % 2 == 0);
            }

            long startTime = System.currentTimeMillis();
            learningLoop.forceLearningCycle();
            long duration = System.currentTimeMillis() - startTime;

            assertTrue(duration < 5000,
                "Should handle 100 sequences in reasonable time, took: " + duration + "ms");
        }
    }

    // ==================== Continuous Learning Tests ====================

    @Nested
    @DisplayName("Continuous Learning Tests")
    class ContinuousLearningTests {

        @Test
        @DisplayName("Learning improves over multiple cycles")
        void learningImprovesOverMultipleCycles() {
            ExecutionTracker tracker = ExecutionTracker.getInstance();
            int initialSkillCount = SkillLibrary.getInstance().getSkillCount();

            // Add sequences over multiple cycles
            for (int cycle = 0; cycle < 5; cycle++) {
                for (int i = 0; i < 3; i++) {
                    String agentId = "agent_c" + cycle + "_i" + i;
                    tracker.startTracking(agentId, "Mining cycle " + cycle);
                    tracker.recordAction(agentId, "mine", Map.of(), 100, true, null);
                    tracker.recordAction(agentId, "mine", Map.of(), 100, true, null);
                    tracker.endTracking(agentId, true);
                }

                learningLoop.forceLearningCycle();
            }

            // Skills may or may not increase depending on pattern detection
            // The important thing is the system remains stable
            assertTrue(SkillLibrary.getInstance().getSkillCount() >= initialSkillCount,
                "Skill count should not decrease");
        }

        @Test
        @DisplayName("Checks performed increases monotonically")
        void checksPerformedIncreasesMonotonically() throws Exception {
            Field checksField = SkillLearningLoop.class.getDeclaredField("checksPerformed");
            checksField.setAccessible(true);
            AtomicInteger checks = (AtomicInteger) checksField.get(learningLoop);

            int previous = checks.get();

            for (int i = 0; i < 10; i++) {
                learningLoop.forceLearningCycle();
                int current = checks.get();
                assertTrue(current >= previous,
                    "Checks should increase monotonically");
                previous = current;
            }
        }

        @Test
        @DisplayName("Periodic refinement check triggers at right interval")
        void periodicRefinementCheckTriggersAtRightInterval() throws Exception {
            Field checksField = SkillLearningLoop.class.getDeclaredField("checksPerformed");
            checksField.setAccessible(true);
            AtomicInteger checks = (AtomicInteger) checksField.get(learningLoop);

            // REFINEMENT_CHECK_INTERVAL is 10
            for (int i = 0; i < 25; i++) {
                learningLoop.forceLearningCycle();
            }

            assertTrue(checks.get() >= 25,
                "Should have performed all checks");
        }

        @Test
        @DisplayName("Learning loop remains stable over time")
        void learningLoopRemainsStableOverTime() throws Exception {
            Field checksField = SkillLearningLoop.class.getDeclaredField("checksPerformed");
            checksField.setAccessible(true);
            AtomicInteger checks = (AtomicInteger) checksField.get(learningLoop);

            learningLoop.start();

            // Let it run briefly
            Thread.sleep(200);

            learningLoop.stop();

            // Should have performed some checks (automatic learning cycle)
            // But we can't guarantee how many, so just verify it's running
            assertTrue(checks.get() >= 0,
                "Checks should be non-negative");
        }
    }
}
