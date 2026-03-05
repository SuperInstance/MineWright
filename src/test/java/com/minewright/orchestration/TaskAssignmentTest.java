package com.minewright.orchestration;

import com.minewright.action.Task;
import com.minewright.testutil.TaskBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for {@link TaskAssignment}.
 *
 * <p>Tests cover the task assignment lifecycle including:</p>
 * <ul>
 *   <li>Task creation and initialization</li>
 *   <li>State transitions and validation</li>
 *   <li>Progress tracking</li>
 *   <li>Assignment and reassignment</li>
 *   <li>Completion and failure handling</li>
 *   <li>Dependency management</li>
 *   <li>Duration calculation</li>
 *   <li>Builder pattern</li>
 *   <li>Thread safety</li>
 *   <li>Edge cases</li>
 * </ul>
 *
 * @see TaskAssignment
 */
@DisplayName("Task Assignment Tests")
class TaskAssignmentTest {

    private static final String FOREMAN_ID = "foreman";
    private static final String PLAN_ID = "plan-123";
    private Task sampleTask;
    private TaskAssignment assignment;

    @BeforeEach
    void setUp() {
        sampleTask = TaskBuilder.aTask("mine")
            .withBlock("stone")
            .withQuantity(64)
            .build();
        assignment = new TaskAssignment(FOREMAN_ID, sampleTask, PLAN_ID);
    }

    // ==================== Creation Tests ====================

    @Nested
    @DisplayName("Task Creation Tests")
    class CreationTests {

        @Test
        @DisplayName("Create task assignment with valid parameters")
        void createWithValidParameters() {
            assertNotNull(assignment.getAssignmentId());
            assertEquals(FOREMAN_ID, assignment.getForemanId());
            assertEquals(PLAN_ID, assignment.getParentPlanId());
            assertEquals(sampleTask, assignment.getOriginalTask());
            assertEquals("mine", assignment.getTaskDescription());
        }

        @Test
        @DisplayName("Task assignment has unique ID")
        void taskHasUniqueId() {
            TaskAssignment assignment2 = new TaskAssignment(FOREMAN_ID, sampleTask, PLAN_ID);

            assertNotEquals(assignment.getAssignmentId(), assignment2.getAssignmentId(),
                "Each assignment should have unique ID");
        }

        @Test
        @DisplayName("Initial state is PENDING")
        void initialStateIsPending() {
            assertEquals(TaskAssignment.State.PENDING, assignment.getState());
        }

        @Test
        @DisplayName("Initial progress is zero")
        void initialProgressIsZero() {
            assertEquals(0, assignment.getProgressPercent());
        }

        @Test
        @DisplayName("Initial retry count is zero")
        void initialRetryCountIsZero() {
            assertEquals(0, assignment.getRetryCount());
        }

        @Test
        @DisplayName("Initial status message")
        void initialStatusMessage() {
            assertEquals("Pending assignment", assignment.getStatusMessage());
        }

        @Test
        @DisplayName("Task parameters are preserved")
        void taskParametersPreserved() {
            Map<String, Object> params = assignment.getParameters();

            assertEquals("stone", params.get("block"));
            assertEquals(64, params.get("quantity"));
        }

        @Test
        @DisplayName("Created timestamp is set")
        void createdTimestampSet() {
            assertNotNull(assignment.getCreatedAt());
            Instant before = Instant.now().minusSeconds(1);
            assertTrue(assignment.getCreatedAt().isAfter(before),
                "Created time should be recent");
        }

        @Test
        @DisplayName("Priority is NORMAL by default")
        void defaultPriority() {
            assertEquals(TaskAssignment.Priority.NORMAL, assignment.getPriority());
        }

        @Test
        @DisplayName("No assigned worker initially")
        void noAssignedWorkerInitially() {
            assertNull(assignment.getAssignedWorkerId());
        }
    }

    // ==================== State Transition Tests ====================

    @Nested
    @DisplayName("State Transition Tests")
    class StateTransitionTests {

        @Test
        @DisplayName("Assign task transitions to ASSIGNED")
        void assignTransitionsToAssigned() {
            assignment.assignTo("worker1");

            assertEquals(TaskAssignment.State.ASSIGNED, assignment.getState());
            assertEquals("worker1", assignment.getAssignedWorkerId());
            assertNotNull(assignment.getAssignedAt());
        }

        @Test
        @DisplayName("Assign updates status message")
        void assignUpdatesStatus() {
            assignment.assignTo("worker1");

            assertEquals("Assigned to worker1", assignment.getStatusMessage());
        }

        @Test
        @DisplayName("Accept task transitions to ACCEPTED")
        void acceptTransitionsToAccepted() {
            assignment.assignTo("worker1");
            assignment.accept();

            assertEquals(TaskAssignment.State.ACCEPTED, assignment.getState());
            assertNotNull(assignment.getStartedAt());
        }

        @Test
        @DisplayName("Accept without assign throws exception")
        void acceptWithoutAssignThrows() {
            assertThrows(IllegalStateException.class, () -> assignment.accept(),
                "Cannot accept unassigned task");
        }

        @Test
        @DisplayName("Start transitions to IN_PROGRESS")
        void startTransitionsToInProgress() {
            assignment.assignTo("worker1");
            assignment.accept();
            assignment.start();

            assertEquals(TaskAssignment.State.IN_PROGRESS, assignment.getState());
            assertEquals("Execution started", assignment.getStatusMessage());
        }

        @Test
        @DisplayName("Start without accept throws exception")
        void startWithoutAcceptThrows() {
            assignment.assignTo("worker1");

            assertThrows(IllegalStateException.class, () -> assignment.start(),
                "Cannot start unaccepted task");
        }

        @Test
        @DisplayName("Complete transitions to COMPLETED")
        void completeTransitionsToCompleted() {
            assignment.assignTo("worker1");
            assignment.complete("Task finished successfully");

            assertEquals(TaskAssignment.State.COMPLETED, assignment.getState());
            assertTrue(assignment.isSuccess());
            assertEquals("Task finished successfully", assignment.getResult());
            assertEquals(100, assignment.getProgressPercent());
            assertNotNull(assignment.getCompletedAt());
        }

        @Test
        @DisplayName("Complete sets success flag")
        void completeSetsSuccessFlag() {
            assignment.assignTo("worker1");
            assignment.complete("Done");

            assertTrue(assignment.isSuccess());
        }

        @Test
        @DisplayName("Fail transitions to FAILED")
        void failTransitionsToFailed() {
            assignment.assignTo("worker1");
            assignment.fail("Path blocked by obstacle");

            assertEquals(TaskAssignment.State.FAILED, assignment.getState());
            assertFalse(assignment.isSuccess());
            assertEquals("Path blocked by obstacle", assignment.getFailureReason());
            assertNotNull(assignment.getCompletedAt());
        }

        @Test
        @DisplayName("Fail sets failure reason")
        void failSetsFailureReason() {
            assignment.assignTo("worker1");
            String reason = "Out of materials";
            assignment.fail(reason);

            assertEquals(reason, assignment.getFailureReason());
        }

        @Test
        @DisplayName("Cancel transitions to CANCELLED")
        void cancelTransitionsToCancelled() {
            assignment.assignTo("worker1");
            assignment.cancel("Cancelled by user");

            assertEquals(TaskAssignment.State.CANCELLED, assignment.getState());
            assertNotNull(assignment.getCompletedAt());
        }

        @Test
        @DisplayName("Reassign transitions to REASSIGNED")
        void reassignTransitionsToReassigned() {
            assignment.assignTo("worker1");
            assignment.reassign("worker2", "Worker1 unavailable");

            assertEquals(TaskAssignment.State.REASSIGNED, assignment.getState());
            assertEquals("worker2", assignment.getAssignedWorkerId());
            assertEquals(1, assignment.getRetryCount());
        }

        @Test
        @DisplayName("Reassign from REASSIGNED state")
        void reassignFromReassignedState() {
            assignment.assignTo("worker1");
            assignment.reassign("worker2", "First retry");
            assertEquals(1, assignment.getRetryCount());

            assignment.assignTo("worker2");
            assignment.reassign("worker3", "Second retry");

            assertEquals(TaskAssignment.State.REASSIGNED, assignment.getState());
            assertEquals(2, assignment.getRetryCount());
        }
    }

    // ==================== Progress Tracking Tests ====================

    @Nested
    @DisplayName("Progress Tracking Tests")
    class ProgressTrackingTests {

        @Test
        @DisplayName("Update progress in ACCEPTED state")
        void updateProgressInAccepted() {
            assignment.assignTo("worker1");
            assignment.accept();

            assignment.updateProgress(50, "Half done");

            assertEquals(50, assignment.getProgressPercent());
            assertEquals("Half done", assignment.getStatusMessage());
        }

        @Test
        @DisplayName("Update progress in IN_PROGRESS state")
        void updateProgressInProgress() {
            assignment.assignTo("worker1");
            assignment.accept();
            assignment.start();

            assignment.updateProgress(75, "Almost done");

            assertEquals(75, assignment.getProgressPercent());
        }

        @Test
        @DisplayName("Update progress caps at 100")
        void updateProgressCapsAt100() {
            assignment.assignTo("worker1");
            assignment.accept();

            assignment.updateProgress(150, "Over 100");

            assertEquals(100, assignment.getProgressPercent(),
                "Progress should cap at 100");
        }

        @Test
        @DisplayName("Update progress floors at 0")
        void updateProgressFloorsAt0() {
            assignment.assignTo("worker1");
            assignment.accept();

            assignment.updateProgress(-10, "Negative progress");

            assertEquals(0, assignment.getProgressPercent(),
                "Progress should floor at 0");
        }

        @Test
        @DisplayName("Update progress in terminal state is ignored")
        void updateProgressInTerminalStateIgnored() {
            assignment.assignTo("worker1");
            assignment.complete("Done");

            int progressBefore = assignment.getProgressPercent();
            assignment.updateProgress(50, "Should be ignored");

            assertEquals(progressBefore, assignment.getProgressPercent(),
                "Progress should not change in terminal state");
        }

        @Test
        @DisplayName("Update progress with null status")
        void updateProgressWithNullStatus() {
            assignment.assignTo("worker1");
            assignment.accept();

            assignment.updateProgress(25, null);

            assertEquals(25, assignment.getProgressPercent());
        }

        @Test
        @DisplayName("Multiple progress updates")
        void multipleProgressUpdates() {
            assignment.assignTo("worker1");
            assignment.accept();

            assignment.updateProgress(25, "Quarter done");
            assertEquals(25, assignment.getProgressPercent());

            assignment.updateProgress(50, "Half done");
            assertEquals(50, assignment.getProgressPercent());

            assignment.updateProgress(75, "Three quarters");
            assertEquals(75, assignment.getProgressPercent());
        }
    }

    // ==================== Dependency Tests ====================

    @Nested
    @DisplayName("Dependency Tests")
    class DependencyTests {

        private TaskAssignment dependency1;
        private TaskAssignment dependency2;

        @BeforeEach
        void setUp() {
            Task task1 = TaskBuilder.aTask("craft").withItem("stick").build();
            Task task2 = TaskBuilder.aTask("craft").withItem("plank").build();

            dependency1 = new TaskAssignment(FOREMAN_ID, task1, PLAN_ID);
            dependency2 = new TaskAssignment(FOREMAN_ID, task2, PLAN_ID);

            // Mark dependencies as completed
            dependency1.assignTo("worker1");
            dependency1.complete("Crafted sticks");

            dependency2.assignTo("worker1");
            dependency2.complete("Crafted planks");
        }

        @Test
        @DisplayName("Add dependency")
        void addDependency() {
            assignment.addDependency(dependency1);

            assertTrue(assignment.getDependencies().containsKey(dependency1.getAssignmentId()));
            assertFalse(assignment.areDependenciesMet(),
                "Should not be met until dependency is checked");
        }

        @Test
        @DisplayName("Add multiple dependencies")
        void addMultipleDependencies() {
            assignment.addDependency(dependency1);
            assignment.addDependency(dependency2);

            assertEquals(2, assignment.getDependencies().size());
        }

        @Test
        @DisplayName("Check dependencies with all completed")
        void checkDependenciesAllCompleted() {
            assignment.addDependency(dependency1);
            assignment.addDependency(dependency2);

            assertTrue(assignment.checkDependencies(),
                "All dependencies should be met");
        }

        @Test
        @DisplayName("Check dependencies with incomplete")
        void checkDependenciesWithIncomplete() {
            Task incompleteTask = TaskBuilder.aTask("mine").withBlock("stone").build();
            TaskAssignment incompleteDependency = new TaskAssignment(FOREMAN_ID, incompleteTask, PLAN_ID);
            incompleteDependency.assignTo("worker1");

            assignment.addDependency(dependency1);
            assignment.addDependency(incompleteDependency);

            assertFalse(assignment.checkDependencies(),
                "Should not be met with incomplete dependency");
        }

        @Test
        @DisplayName("Check dependencies with empty map")
        void checkDependenciesWithEmptyMap() {
            assertTrue(assignment.checkDependencies(),
                "No dependencies means they're met");
        }

        @Test
        @DisplayName("Check dependencies updates flag")
        void checkDependenciesUpdatesFlag() {
            assignment.addDependency(dependency1);

            assignment.checkDependencies();
            assertTrue(assignment.areDependenciesMet());
        }

        @Test
        @DisplayName("Dependencies are preserved in map")
        void dependenciesPreservedInMap() {
            assignment.addDependency(dependency1);
            assignment.addDependency(dependency2);

            Map<String, TaskAssignment> deps = assignment.getDependencies();

            assertEquals(dependency1, deps.get(dependency1.getAssignmentId()));
            assertEquals(dependency2, deps.get(dependency2.getAssignmentId()));
        }

        @Test
        @DisplayName("Failed dependency prevents completion")
        void failedDependencyPreventsCompletion() {
            Task failedTask = TaskBuilder.aTask("mine").withBlock("stone").build();
            TaskAssignment failedDependency = new TaskAssignment(FOREMAN_ID, failedTask, PLAN_ID);
            failedDependency.assignTo("worker1");
            failedDependency.fail("Cannot reach stone");

            assignment.addDependency(dependency1);
            assignment.addDependency(failedDependency);

            assertFalse(assignment.checkDependencies(),
                "Failed dependency should prevent meeting dependencies");
        }
    }

    // ==================== Duration Calculation Tests ====================

    @Nested
    @DisplayName("Duration Calculation Tests")
    class DurationCalculationTests {

        @Test
        @DisplayName("Duration is zero before start")
        void durationZeroBeforeStart() {
            assertEquals(0, assignment.getDurationMs(),
                "Duration should be zero before starting");
        }

        @Test
        @DisplayName("Duration increases after start")
        void durationIncreasesAfterStart() throws InterruptedException {
            assignment.assignTo("worker1");
            assignment.accept();

            assignment.start();
            Thread.sleep(10);

            assertTrue(assignment.getDurationMs() > 0,
                "Duration should be positive after start");
        }

        @Test
        @DisplayName("Duration is final after completion")
        void durationFinalAfterCompletion() {
            assignment.assignTo("worker1");
            assignment.accept();
            assignment.start();
            assignment.complete("Done");

            long duration1 = assignment.getDurationMs();
            long duration2 = assignment.getDurationMs();

            assertEquals(duration1, duration2,
                "Duration should be constant after completion");
        }

        @Test
        @DisplayName("Duration is calculated from start to complete")
        void durationFromStartToComplete() throws InterruptedException {
            assignment.assignTo("worker1");
            assignment.accept();

            assignment.start();
            Thread.sleep(50);
            assignment.complete("Done");

            assertTrue(assignment.getDurationMs() >= 50,
                "Duration should reflect elapsed time");
        }

        @Test
        @DisplayName("Duration with failure")
        void durationWithFailure() throws InterruptedException {
            assignment.assignTo("worker1");
            assignment.accept();
            assignment.start();
            Thread.sleep(20);
            assignment.fail("Error");

            assertTrue(assignment.getDurationMs() >= 20,
                "Duration should be calculated even on failure");
        }

        @Test
        @DisplayName("Duration with cancellation")
        void durationWithCancellation() throws InterruptedException {
            assignment.assignTo("worker1");
            assignment.accept();
            assignment.start();
            Thread.sleep(15);
            assignment.cancel("Cancelled");

            assertTrue(assignment.getDurationMs() >= 15,
                "Duration should be calculated even on cancellation");
        }
    }

    // ==================== State Property Tests ====================

    @Nested
    @DisplayName("State Property Tests")
    class StatePropertyTests {

        @Test
        @DisplayName("Is terminal for terminal states")
        void isTerminalForTerminalStates() {
            assignment.assignTo("worker1");

            assertFalse(assignment.isTerminal(),
                "ASSIGNED should not be terminal");

            assignment.complete("Done");
            assertTrue(assignment.isTerminal(),
                "COMPLETED should be terminal");
        }

        @Test
        @DisplayName("All terminal states are correctly identified")
        void allTerminalStatesIdentified() {
            TaskAssignment[] assignments = new TaskAssignment[4];

            for (int i = 0; i < 4; i++) {
                assignments[i] = new TaskAssignment(FOREMAN_ID, sampleTask, PLAN_ID);
                assignments[i].assignTo("worker1");
            }

            assignments[0].complete("Done");
            assignments[1].fail("Error");
            assignments[2].cancel("Cancelled");

            assertTrue(assignments[0].isTerminal());
            assertTrue(assignments[1].isTerminal());
            assertTrue(assignments[2].isTerminal());

            // REASSIGNED is not terminal
            assignments[3].reassign("worker2", "Retry");
            assertFalse(assignments[3].isTerminal());
        }

        @Test
        @DisplayName("Is active for active states")
        void isActiveForActiveStates() {
            assignment.assignTo("worker1");
            assertFalse(assignment.isActive(),
                "ASSIGNED should not be active");

            assignment.accept();
            assertTrue(assignment.isActive(),
                "ACCEPTED should be active");

            assignment.start();
            assertTrue(assignment.isActive(),
                "IN_PROGRESS should be active");
        }

        @Test
        @DisplayName("All active states are correctly identified")
        void allActiveStatesIdentified() {
            TaskAssignment[] assignments = new TaskAssignment[2];

            for (int i = 0; i < 2; i++) {
                assignments[i] = new TaskAssignment(FOREMAN_ID, sampleTask, PLAN_ID);
                assignments[i].assignTo("worker1");
                assignments[i].accept();
            }

            assertTrue(assignments[0].isActive(),
                "ACCEPTED should be active");

            assignments[1].start();
            assertTrue(assignments[1].isActive(),
                "IN_PROGRESS should be active");
        }

        @Test
        @DisplayName("State enum values are accessible")
        void stateEnumValues() {
            TaskAssignment.State[] states = TaskAssignment.State.values();

            assertEquals(8, states.length, "Should have 8 states");

            for (TaskAssignment.State state : states) {
                assertNotNull(state.getCode());
                assertNotNull(state.toString());
            }
        }

        @Test
        @DisplayName("State codes are correct")
        void stateCodesAreCorrect() {
            assertEquals("pending", TaskAssignment.State.PENDING.getCode());
            assertEquals("assigned", TaskAssignment.State.ASSIGNED.getCode());
            assertEquals("accepted", TaskAssignment.State.ACCEPTED.getCode());
            assertEquals("in_progress", TaskAssignment.State.IN_PROGRESS.getCode());
            assertEquals("completed", TaskAssignment.State.COMPLETED.getCode());
            assertEquals("failed", TaskAssignment.State.FAILED.getCode());
            assertEquals("cancelled", TaskAssignment.State.CANCELLED.getCode());
            assertEquals("reassigned", TaskAssignment.State.REASSIGNED.getCode());
        }
    }

    // ==================== Priority Tests ====================

    @Nested
    @DisplayName("Priority Tests")
    class PriorityTests {

        @Test
        @DisplayName("Priority levels are correct")
        void priorityLevels() {
            assertEquals(1, TaskAssignment.Priority.LOW.getLevel());
            assertEquals(5, TaskAssignment.Priority.NORMAL.getLevel());
            assertEquals(10, TaskAssignment.Priority.HIGH.getLevel());
            assertEquals(20, TaskAssignment.Priority.URGENT.getLevel());
            assertEquals(50, TaskAssignment.Priority.CRITICAL.getLevel());
        }

        @Test
        @DisplayName("Priority enum values")
        void priorityValues() {
            TaskAssignment.Priority[] priorities = TaskAssignment.Priority.values();

            assertEquals(5, priorities.length);
        }
    }

    // ==================== Builder Tests ====================

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("Build with all fields")
        void buildWithAllFields() {
            TaskAssignment built = new TaskAssignment.Builder()
                .foremanId(FOREMAN_ID)
                .task(sampleTask)
                .parentPlanId(PLAN_ID)
                .priority(TaskAssignment.Priority.HIGH)
                .assignedWorkerId("worker1")
                .build();

            assertEquals(FOREMAN_ID, built.getForemanId());
            assertEquals(sampleTask, built.getOriginalTask());
            assertEquals(PLAN_ID, built.getParentPlanId());
            assertEquals(TaskAssignment.Priority.HIGH, built.getPriority());
            assertEquals("worker1", built.getAssignedWorkerId());
            assertEquals(TaskAssignment.State.ASSIGNED, built.getState());
        }

        @Test
        @DisplayName("Build without required fields throws")
        void buildWithoutRequiredFieldsThrows() {
            assertThrows(IllegalStateException.class, () -> {
                new TaskAssignment.Builder()
                    .foremanId(FOREMAN_ID)
                    .build();
            }, "Builder should require task");

            assertThrows(IllegalStateException.class, () -> {
                new TaskAssignment.Builder()
                    .task(sampleTask)
                    .build();
            }, "Builder should require foremanId");
        }

        @Test
        @DisplayName("Build with minimum fields")
        void buildWithMinimumFields() {
            TaskAssignment built = new TaskAssignment.Builder()
                .foremanId(FOREMAN_ID)
                .task(sampleTask)
                .build();

            assertEquals(FOREMAN_ID, built.getForemanId());
            assertEquals(sampleTask, built.getOriginalTask());
            assertEquals(TaskAssignment.State.PENDING, built.getState());
        }

        @Test
        @DisplayName("Builder priority defaults to NORMAL")
        void builderPriorityDefault() {
            TaskAssignment built = new TaskAssignment.Builder()
                .foremanId(FOREMAN_ID)
                .task(sampleTask)
                .build();

            assertEquals(TaskAssignment.Priority.NORMAL, built.getPriority());
        }
    }

    // ==================== Summary and ToString Tests ====================

    @Nested
    @DisplayName("Summary and ToString Tests")
    class SummaryTests {

        @Test
        @DisplayName("GetSummary returns formatted string")
        void getSummaryReturnsFormattedString() {
            assignment.assignTo("worker1");

            String summary = assignment.getSummary();

            assertTrue(summary.contains(assignment.getAssignmentId()));
            assertTrue(summary.contains("mine"));
            assertTrue(summary.contains("worker1"));
            assertTrue(summary.contains("ASSIGNED"));
        }

        @Test
        @DisplayName("ToString returns summary")
        void toStringReturnsSummary() {
            assignment.assignTo("worker1");

            assertEquals(assignment.getSummary(), assignment.toString());
        }

        @Test
        @DisplayName("Summary includes progress")
        void summaryIncludesProgress() {
            assignment.assignTo("worker1");
            assignment.accept();
            assignment.updateProgress(50, "Half done");

            String summary = assignment.getSummary();

            assertTrue(summary.contains("50%"));
        }

        @Test
        @DisplayName("Summary includes status")
        void summaryIncludesStatus() {
            assignment.assignTo("worker1");
            assignment.updateProgress(0, "Custom status");

            String summary = assignment.getSummary();

            assertTrue(summary.contains("Custom status"));
        }
    }

    // ==================== Thread Safety Tests ====================

    @Nested
    @DisplayName("Thread Safety Tests")
    class ThreadSafetyTests {

        @Test
        @DisplayName("Concurrent progress updates")
        void concurrentProgressUpdates() throws InterruptedException {
            assignment.assignTo("worker1");
            assignment.accept();

            int threadCount = 10;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);

            for (int i = 0; i < threadCount; i++) {
                new Thread(() -> {
                    try {
                        startLatch.await();
                        for (int j = 0; j < 10; j++) {
                            assignment.updateProgress(j * 10, "Update " + j);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                }).start();
            }

            startLatch.countDown();
            assertTrue(doneLatch.await(10, TimeUnit.SECONDS));

            // Progress should be between 0 and 100
            assertTrue(assignment.getProgressPercent() >= 0);
            assertTrue(assignment.getProgressPercent() <= 100);
        }

        @Test
        @DisplayName("Concurrent state checks")
        void concurrentStateChecks() throws InterruptedException {
            int threadCount = 20;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);

            for (int i = 0; i < threadCount; i++) {
                new Thread(() -> {
                    try {
                        startLatch.await();
                        for (int j = 0; j < 50; j++) {
                            assignment.isTerminal();
                            assignment.isActive();
                            assignment.getState();
                            assignment.getProgressPercent();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                }).start();
            }

            startLatch.countDown();
            assertTrue(doneLatch.await(10, TimeUnit.SECONDS));

            // Should complete without exception
            assertEquals(TaskAssignment.State.PENDING, assignment.getState());
        }

        @Test
        @DisplayName("Concurrent dependency checks")
        void concurrentDependencyChecks() throws InterruptedException {
            Task depTask = TaskBuilder.aTask("craft").withItem("stick").build();
            TaskAssignment dependency = new TaskAssignment(FOREMAN_ID, depTask, PLAN_ID);
            dependency.assignTo("worker1");
            dependency.complete("Done");

            assignment.addDependency(dependency);

            int threadCount = 15;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);

            for (int i = 0; i < threadCount; i++) {
                new Thread(() -> {
                    try {
                        startLatch.await();
                        for (int j = 0; j < 20; j++) {
                            assignment.checkDependencies();
                            assignment.areDependenciesMet();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                }).start();
            }

            startLatch.countDown();
            assertTrue(doneLatch.await(10, TimeUnit.SECONDS));

            assertTrue(assignment.areDependenciesMet());
        }
    }

    // ==================== Edge Cases Tests ====================

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Handle null result in complete")
        void handleNullResultInComplete() {
            assignment.assignTo("worker1");
            assignment.complete(null);

            assertNull(assignment.getResult());
        }

        @Test
        @DisplayName("Handle null reason in fail")
        void handleNullReasonInFail() {
            assignment.assignTo("worker1");
            assignment.fail(null);

            assertNull(assignment.getFailureReason());
        }

        @Test
        @DisplayName("Handle null reason in cancel")
        void handleNullReasonInCancel() {
            assignment.assignTo("worker1");
            assignment.cancel(null);

            assertTrue(assignment.isTerminal());
        }

        @Test
        @DisplayName("Handle empty task parameters")
        void handleEmptyTaskParameters() {
            Task emptyTask = new Task("test", Map.of());
            TaskAssignment emptyAssignment = new TaskAssignment(FOREMAN_ID, emptyTask, PLAN_ID);

            assertTrue(emptyAssignment.getParameters().isEmpty());
        }

        @Test
        @DisplayName("Handle very long status messages")
        void handleVeryLongStatus() {
            assignment.assignTo("worker1");
            String longStatus = "A".repeat(1000);

            assignment.updateProgress(50, longStatus);

            assertEquals(longStatus, assignment.getStatusMessage());
        }

        @Test
        @DisplayName("Handle special characters in status")
        void handleSpecialCharactersInStatus() {
            assignment.assignTo("worker1");
            String specialStatus = "Status\n\t\r: Test & more";

            assignment.updateProgress(50, specialStatus);

            assertEquals(specialStatus, assignment.getStatusMessage());
        }

        @Test
        @DisplayName("Multiple completions handled gracefully")
        void multipleCompletions() {
            assignment.assignTo("worker1");
            assignment.complete("First completion");

            assertEquals(TaskAssignment.State.COMPLETED, assignment.getState());

            // Second complete should be ignored or handled
            assignment.complete("Second completion");

            assertEquals(TaskAssignment.State.COMPLETED, assignment.getState());
        }

        @Test
        @DisplayName("Complete then fail keeps complete state")
        void completeThenFail() {
            assignment.assignTo("worker1");
            assignment.complete("Done");
            assignment.fail("Error");

            assertEquals(TaskAssignment.State.COMPLETED, assignment.getState());
        }

        @Test
        @DisplayName("Fail then complete keeps fail state")
        void failThenComplete() {
            assignment.assignTo("worker1");
            assignment.fail("Error");
            assignment.complete("Done");

            assertEquals(TaskAssignment.State.FAILED, assignment.getState());
        }
    }

    // ==================== Integration Tests ====================

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Full task lifecycle")
        void fullTaskLifecycle() {
            // Start
            assertEquals(TaskAssignment.State.PENDING, assignment.getState());
            assertEquals(0, assignment.getProgressPercent());

            // Assign
            assignment.assignTo("worker1");
            assertEquals(TaskAssignment.State.ASSIGNED, assignment.getState());

            // Accept
            assignment.accept();
            assertEquals(TaskAssignment.State.ACCEPTED, assignment.getState());

            // Start
            assignment.start();
            assertEquals(TaskAssignment.State.IN_PROGRESS, assignment.getState());

            // Progress
            assignment.updateProgress(25, "Quarter done");
            assertEquals(25, assignment.getProgressPercent());

            assignment.updateProgress(50, "Half done");
            assertEquals(50, assignment.getProgressPercent());

            assignment.updateProgress(75, "Three quarters");
            assertEquals(75, assignment.getProgressPercent());

            // Complete
            assignment.complete("Task finished");
            assertEquals(TaskAssignment.State.COMPLETED, assignment.getState());
            assertEquals(100, assignment.getProgressPercent());
            assertTrue(assignment.isSuccess());
            assertTrue(assignment.isTerminal());
        }

        @Test
        @DisplayName("Task failure with retry workflow")
        void failureWithRetryWorkflow() {
            // First attempt
            assignment.assignTo("worker1");
            assignment.accept();
            assignment.start();
            assignment.updateProgress(50, "Half done");
            assignment.fail("Worker disconnected");

            assertEquals(TaskAssignment.State.FAILED, assignment.getState());
            assertEquals(1, assignment.getRetryCount());

            // Retry
            TaskAssignment retry = new TaskAssignment(FOREMAN_ID, sampleTask, PLAN_ID);
            retry.assignTo("worker2");
            retry.accept();
            retry.start();
            retry.updateProgress(100, "Complete");
            retry.complete("Success on retry");

            assertEquals(TaskAssignment.State.COMPLETED, retry.getState());
            assertTrue(retry.isSuccess());
        }

        @Test
        @DisplayName("Task with dependencies workflow")
        void taskWithDependenciesWorkflow() {
            // Create dependencies
            Task dep1Task = TaskBuilder.aTask("craft").withItem("stick").build();
            Task dep2Task = TaskBuilder.aTask("craft").withItem("plank").build();

            TaskAssignment dep1 = new TaskAssignment(FOREMAN_ID, dep1Task, PLAN_ID);
            TaskAssignment dep2 = new TaskAssignment(FOREMAN_ID, dep2Task, PLAN_ID);

            // Add as dependencies
            assignment.addDependency(dep1);
            assignment.addDependency(dep2);

            // Dependencies not met initially
            assertFalse(assignment.areDependenciesMet());

            // Complete first dependency
            dep1.assignTo("worker1");
            dep1.complete("Crafted sticks");

            assignment.checkDependencies();
            assertFalse(assignment.areDependenciesMet(),
                "Should still not be met with one incomplete");

            // Complete second dependency
            dep2.assignTo("worker2");
            dep2.complete("Crafted planks");

            assignment.checkDependencies();
            assertTrue(assignment.areDependenciesMet(),
                "Should be met when all complete");

            // Now can proceed
            assignment.assignTo("worker3");
            assignment.accept();
            assignment.start();
            assignment.complete("Main task complete");

            assertEquals(TaskAssignment.State.COMPLETED, assignment.getState());
        }
    }
}
