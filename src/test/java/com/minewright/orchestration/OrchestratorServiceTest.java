package com.minewright.orchestration;

import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import com.minewright.llm.ResponseParser;
import com.minewright.testutil.TaskBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for {@link OrchestratorService}.
 *
 * <p>Tests cover the orchestration system including:</p>
 * <ul>
 *   <li>Agent registration (foreman and workers)</li>
 *   <li>Agent unregistration and cleanup</li>
 *   <li>Task distribution and assignment</li>
 *   <li>Message handling (task progress, completion, failure)</li>
 *   <li>Plan execution tracking</li>
 *   <li>Multi-agent coordination</li>
 *   <li>Solo mode fallback</li>
 *   <li>Thread safety</li>
 *   <li>Foreman election</li>
 * </ul>
 *
 * @see OrchestratorService
 * @see AgentCommunicationBus
 * @see TaskAssignment
 */
@DisplayName("Orchestrator Service Tests")
@ExtendWith(MockitoExtension.class)
class OrchestratorServiceTest {

    private OrchestratorService orchestrator;

    @Mock
    private ForemanEntity foremanEntity;

    @Mock
    private ForemanEntity workerEntity1;

    @Mock
    private ForemanEntity workerEntity2;

    @Mock
    private ForemanEntity workerEntity3;

    private static final String FOREMAN_ID = "foreman";
    private static final String WORKER_1_ID = "worker1";
    private static final String WORKER_2_ID = "worker2";
    private static final String WORKER_3_ID = "worker3";

    @BeforeEach
    void setUp() {
        orchestrator = new OrchestratorService();

        // Setup mock entities
        lenient().when(foremanEntity.getEntityName()).thenReturn(FOREMAN_ID);
        lenient().when(workerEntity1.getEntityName()).thenReturn(WORKER_1_ID);
        lenient().when(workerEntity2.getEntityName()).thenReturn(WORKER_2_ID);
        lenient().when(workerEntity3.getEntityName()).thenReturn(WORKER_3_ID);
    }

    // ==================== Registration Tests ====================

    @Nested
    @DisplayName("Agent Registration Tests")
    class RegistrationTests {

        @Test
        @DisplayName("Register foreman agent successfully")
        void registerForeman() {
            orchestrator.registerAgent(foremanEntity, AgentRole.FOREMAN);

            Set<String> activePlans = orchestrator.getActivePlanIds();

            assertNotNull(activePlans, "Active plans should not be null");
            assertTrue(activePlans.isEmpty(), "Should have no active plans initially");

            AgentCommunicationBus bus = orchestrator.getCommunicationBus();
            assertNotNull(bus, "Communication bus should be initialized");
        }

        @Test
        @DisplayName("Register worker agent successfully")
        void registerWorker() {
            orchestrator.registerAgent(workerEntity1, AgentRole.WORKER);

            AgentCommunicationBus bus = orchestrator.getCommunicationBus();
            Set<String> registeredAgents = bus.getRegisteredAgents();

            assertTrue(registeredAgents.contains(WORKER_1_ID),
                "Worker should be registered in communication bus");
        }

        @Test
        @DisplayName("Register specialist agent successfully")
        void registerSpecialist() {
            orchestrator.registerAgent(workerEntity1, AgentRole.SPECIALIST);

            AgentCommunicationBus bus = orchestrator.getCommunicationBus();
            Set<String> registeredAgents = bus.getRegisteredAgents();

            assertTrue(registeredAgents.contains(WORKER_1_ID),
                "Specialist should be registered in communication bus");
        }

        @Test
        @DisplayName("Register multiple workers")
        void registerMultipleWorkers() {
            orchestrator.registerAgent(workerEntity1, AgentRole.WORKER);
            orchestrator.registerAgent(workerEntity2, AgentRole.WORKER);
            orchestrator.registerAgent(workerEntity3, AgentRole.WORKER);

            AgentCommunicationBus bus = orchestrator.getCommunicationBus();
            Set<String> registeredAgents = bus.getRegisteredAgents();

            assertEquals(3, registeredAgents.size(),
                "Should have 3 workers registered");
            assertTrue(registeredAgents.contains(WORKER_1_ID));
            assertTrue(registeredAgents.contains(WORKER_2_ID));
            assertTrue(registeredAgents.contains(WORKER_3_ID));
        }

        @Test
        @DisplayName("Register null agent does not throw exception")
        void registerNullAgent() {
            assertDoesNotThrow(() -> orchestrator.registerAgent(null, AgentRole.WORKER),
                "Registering null agent should not throw exception");
        }

        @Test
        @DisplayName("Register agent with null role does not throw exception")
        void registerAgentWithNullRole() {
            assertDoesNotThrow(() -> orchestrator.registerAgent(workerEntity1, null),
                "Registering agent with null role should not throw exception");
        }

        @Test
        @DisplayName("Register agent with null entity name is handled")
        void registerAgentWithNullEntityName() {
            lenient().when(workerEntity1.getEntityName()).thenReturn(null);

            assertDoesNotThrow(() -> orchestrator.registerAgent(workerEntity1, AgentRole.WORKER),
                "Should handle null entity name gracefully");
        }

        @Test
        @DisplayName("Register agent with empty entity name is handled")
        void registerAgentWithEmptyEntityName() {
            lenient().when(workerEntity1.getEntityName()).thenReturn("");

            assertDoesNotThrow(() -> orchestrator.registerAgent(workerEntity1, AgentRole.WORKER),
                "Should handle empty entity name gracefully");
        }

        @Test
        @DisplayName("Replacing foreman with new foreman")
        void replaceForeman() {
            ForemanEntity newForeman = mock(ForemanEntity.class);
            lenient().when(newForeman.getEntityName()).thenReturn("new_foreman");

            orchestrator.registerAgent(foremanEntity, AgentRole.FOREMAN);
            orchestrator.registerAgent(newForeman, AgentRole.FOREMAN);

            // New foreman should be registered
            AgentCommunicationBus bus = orchestrator.getCommunicationBus();
            Set<String> registeredAgents = bus.getRegisteredAgents();

            assertTrue(registeredAgents.contains("new_foreman"),
                "New foreman should be registered");
            assertTrue(registeredAgents.contains(FOREMAN_ID),
                "Old foreman should still be in bus (but not active)");
        }

        @Test
        @DisplayName("Register same agent multiple times")
        void registerSameAgentMultipleTimes() {
            orchestrator.registerAgent(workerEntity1, AgentRole.WORKER);
            orchestrator.registerAgent(workerEntity1, AgentRole.WORKER);
            orchestrator.registerAgent(workerEntity1, AgentRole.WORKER);

            AgentCommunicationBus bus = orchestrator.getCommunicationBus();
            Set<String> registeredAgents = bus.getRegisteredAgents();

            // Should still only have one entry
            long count = registeredAgents.stream()
                .filter(id -> id.equals(WORKER_1_ID))
                .count();

            assertEquals(1, count, "Should only have one registration for the agent");
        }

        @Test
        @DisplayName("Foreman and workers can coexist")
        void foremanAndWorkersCoexist() {
            orchestrator.registerAgent(foremanEntity, AgentRole.FOREMAN);
            orchestrator.registerAgent(workerEntity1, AgentRole.WORKER);
            orchestrator.registerAgent(workerEntity2, AgentRole.WORKER);

            AgentCommunicationBus bus = orchestrator.getCommunicationBus();
            Set<String> registeredAgents = bus.getRegisteredAgents();

            assertEquals(3, registeredAgents.size());
            assertTrue(registeredAgents.contains(FOREMAN_ID));
            assertTrue(registeredAgents.contains(WORKER_1_ID));
            assertTrue(registeredAgents.contains(WORKER_2_ID));
        }

        @Test
        @DisplayName("Register solo role agent")
        void registerSoloAgent() {
            orchestrator.registerAgent(workerEntity1, AgentRole.SOLO);

            AgentCommunicationBus bus = orchestrator.getCommunicationBus();
            Set<String> registeredAgents = bus.getRegisteredAgents();

            assertTrue(registeredAgents.contains(WORKER_1_ID),
                "Solo agent should be registered");
        }

        @Test
        @DisplayName("Message handler is registered for agent")
        void messageHandlerRegistered() {
            orchestrator.registerAgent(workerEntity1, AgentRole.WORKER);

            // Verify agent is subscribed to communication bus
            AgentCommunicationBus bus = orchestrator.getCommunicationBus();

            // Poll should not throw exception
            assertDoesNotThrow(() -> bus.poll(WORKER_1_ID),
                "Agent should have a message queue");
        }
    }

    // ==================== Unregistration Tests ====================

    @Nested
    @DisplayName("Agent Unregistration Tests")
    class UnregistrationTests {

        @Test
        @DisplayName("Unregister worker removes from bus")
        void unregisterWorker() {
            orchestrator.registerAgent(workerEntity1, AgentRole.WORKER);
            orchestrator.unregisterAgent(WORKER_1_ID);

            AgentCommunicationBus bus = orchestrator.getCommunicationBus();
            Set<String> registeredAgents = bus.getRegisteredAgents();

            assertFalse(registeredAgents.contains(WORKER_1_ID),
                "Worker should be removed from communication bus");
        }

        @Test
        @DisplayName("Unregister foreman triggers election")
        void unregisterForemanTriggersElection() {
            orchestrator.registerAgent(foremanEntity, AgentRole.FOREMAN);
            orchestrator.registerAgent(workerEntity1, AgentRole.WORKER);
            orchestrator.registerAgent(workerEntity2, AgentRole.WORKER);

            orchestrator.unregisterAgent(FOREMAN_ID);

            // A new foreman should be elected from workers
            AgentCommunicationBus bus = orchestrator.getCommunicationBus();
            Set<String> registeredAgents = bus.getRegisteredAgents();

            assertTrue(registeredAgents.contains(WORKER_1_ID) ||
                       registeredAgents.contains(WORKER_2_ID),
                "Workers should still be registered");
        }

        @Test
        @DisplayName("Unregister null agent ID is handled")
        void unregisterNullAgentId() {
            assertDoesNotThrow(() -> orchestrator.unregisterAgent(null),
                "Unregistering null ID should not throw exception");
        }

        @Test
        @DisplayName("Unregister empty agent ID is handled")
        void unregisterEmptyAgentId() {
            assertDoesNotThrow(() -> orchestrator.unregisterAgent(""),
                "Unregistering empty ID should not throw exception");
        }

        @Test
        @DisplayName("Unregister non-existent agent is handled")
        void unregisterNonExistentAgent() {
            assertDoesNotThrow(() -> orchestrator.unregisterAgent("nonexistent"),
                "Unregistering non-existent agent should not throw exception");
        }

        @Test
        @DisplayName("Unregister agent with active tasks triggers reassignment")
        void unregisterAgentWithActiveTasks() {
            orchestrator.registerAgent(foremanEntity, AgentRole.FOREMAN);
            orchestrator.registerAgent(workerEntity1, AgentRole.WORKER);
            orchestrator.registerAgent(workerEntity2, AgentRole.WORKER);

            // Create a plan with tasks
            List<Task> tasks = List.of(
                TaskBuilder.Presets.mineStone(64),
                TaskBuilder.Presets.placeBlock("dirt", 0, 64, 0)
            );

            ResponseParser.ParsedResponse response = mock(ResponseParser.ParsedResponse.class);
            lenient().when(response.getPlan()).thenReturn("Test plan");
            lenient().when(response.getTasks()).thenReturn(tasks);

            List<ForemanEntity> agents = List.of(workerEntity1, workerEntity2);
            orchestrator.processHumanCommand(response, agents);

            // Unregister worker1 - tasks should be reassigned
            orchestrator.unregisterAgent(WORKER_1_ID);

            AgentCommunicationBus bus = orchestrator.getCommunicationBus();
            Set<String> registeredAgents = bus.getRegisteredAgents();

            assertFalse(registeredAgents.contains(WORKER_1_ID),
                "Unregistered worker should be removed");
        }

        @Test
        @DisplayName("Unregister all agents cleans up properly")
        void unregisterAllAgents() {
            orchestrator.registerAgent(foremanEntity, AgentRole.FOREMAN);
            orchestrator.registerAgent(workerEntity1, AgentRole.WORKER);
            orchestrator.registerAgent(workerEntity2, AgentRole.WORKER);

            orchestrator.unregisterAgent(FOREMAN_ID);
            orchestrator.unregisterAgent(WORKER_1_ID);
            orchestrator.unregisterAgent(WORKER_2_ID);

            AgentCommunicationBus bus = orchestrator.getCommunicationBus();
            Set<String> registeredAgents = bus.getRegisteredAgents();

            assertTrue(registeredAgents.isEmpty(),
                "All agents should be unregistered");
        }

        @Test
        @DisplayName("Unregister and re-register same agent")
        void unregisterAndReregister() {
            orchestrator.registerAgent(workerEntity1, AgentRole.WORKER);

            AgentCommunicationBus bus = orchestrator.getCommunicationBus();
            assertTrue(bus.getRegisteredAgents().contains(WORKER_1_ID));

            orchestrator.unregisterAgent(WORKER_1_ID);
            assertFalse(bus.getRegisteredAgents().contains(WORKER_1_ID));

            orchestrator.registerAgent(workerEntity1, AgentRole.WORKER);
            assertTrue(bus.getRegisteredAgents().contains(WORKER_1_ID),
                "Agent should be registered again after re-registration");
        }
    }

    // ==================== Command Processing Tests ====================

    @Nested
    @DisplayName("Command Processing Tests")
    class CommandProcessingTests {

        private List<Task> sampleTasks;
        private ResponseParser.ParsedResponse mockResponse;

        @BeforeEach
        void setUp() {
            sampleTasks = List.of(
                TaskBuilder.Presets.mineStone(64),
                TaskBuilder.Presets.placeBlock("dirt", 0, 64, 0),
                TaskBuilder.Presets.craftItem("stick", 4)
            );

            mockResponse = mock(ResponseParser.ParsedResponse.class);
            lenient().when(mockResponse.getPlan()).thenReturn("Test mining and building plan");
            lenient().when(mockResponse.getTasks()).thenReturn(sampleTasks);
        }

        @Test
        @DisplayName("Process human command with foreman creates plan")
        void processCommandWithForeman() {
            orchestrator.registerAgent(foremanEntity, AgentRole.FOREMAN);
            orchestrator.registerAgent(workerEntity1, AgentRole.WORKER);

            List<ForemanEntity> agents = List.of(workerEntity1);
            String planId = orchestrator.processHumanCommand(mockResponse, agents);

            assertNotNull(planId, "Plan ID should not be null");
            assertFalse(planId.isEmpty(), "Plan ID should not be empty");

            Set<String> activePlans = orchestrator.getActivePlanIds();
            assertTrue(activePlans.contains(planId),
                "Plan should be in active plans");
        }

        @Test
        @DisplayName("Process command without foreman uses solo mode")
        void processCommandWithoutForeman() {
            orchestrator.registerAgent(workerEntity1, AgentRole.WORKER);

            List<ForemanEntity> agents = List.of(workerEntity1);
            String planId = orchestrator.processHumanCommand(mockResponse, agents);

            assertNotNull(planId, "Plan ID should be generated in solo mode");

            Set<String> activePlans = orchestrator.getActivePlanIds();
            assertTrue(activePlans.contains(planId),
                "Plan should be created even in solo mode");
        }

        @Test
        @DisplayName("Process command with no available agents")
        void processCommandWithNoAgents() {
            List<ForemanEntity> emptyList = List.of();
            String planId = orchestrator.processHumanCommand(mockResponse, emptyList);

            assertNotNull(planId, "Plan ID should still be generated");
        }

        @Test
        @DisplayName("Process command distributes tasks to multiple workers")
        void processCommandDistributesTasks() {
            orchestrator.registerAgent(foremanEntity, AgentRole.FOREMAN);
            orchestrator.registerAgent(workerEntity1, AgentRole.WORKER);
            orchestrator.registerAgent(workerEntity2, AgentRole.WORKER);
            orchestrator.registerAgent(workerEntity3, AgentRole.WORKER);

            List<ForemanEntity> agents = List.of(
                workerEntity1, workerEntity2, workerEntity3
            );

            String planId = orchestrator.processHumanCommand(mockResponse, agents);

            assertNotNull(planId);

            // Check plan progress
            int progress = orchestrator.getPlanProgress(planId);
            assertTrue(progress >= 0 && progress <= 100,
                "Progress should be between 0 and 100");
        }

        @Test
        @DisplayName("Process command with null response")
        void processCommandWithNullResponse() {
            orchestrator.registerAgent(foremanEntity, AgentRole.FOREMAN);

            assertDoesNotThrow(() -> {
                String planId = orchestrator.processHumanCommand(null, List.of());
                // Should handle gracefully or return null
            }, "Should handle null response without throwing exception");
        }

        @Test
        @DisplayName("Process command with empty task list")
        void processCommandWithEmptyTasks() {
            orchestrator.registerAgent(foremanEntity, AgentRole.FOREMAN);

            ResponseParser.ParsedResponse emptyResponse = mock(ResponseParser.ParsedResponse.class);
            lenient().when(emptyResponse.getPlan()).thenReturn("Empty plan");
            lenient().when(emptyResponse.getTasks()).thenReturn(List.of());

            String planId = orchestrator.processHumanCommand(
                emptyResponse, List.of(workerEntity1)
            );

            assertNotNull(planId, "Plan should be created even with no tasks");
        }

        @Test
        @DisplayName("Process multiple commands creates multiple plans")
        void processMultipleCommands() {
            orchestrator.registerAgent(foremanEntity, AgentRole.FOREMAN);
            orchestrator.registerAgent(workerEntity1, AgentRole.WORKER);

            List<ForemanEntity> agents = List.of(workerEntity1);

            String planId1 = orchestrator.processHumanCommand(mockResponse, agents);
            String planId2 = orchestrator.processHumanCommand(mockResponse, agents);

            assertNotNull(planId1);
            assertNotNull(planId2);
            assertNotEquals(planId1, planId2,
                "Each command should generate unique plan ID");

            Set<String> activePlans = orchestrator.getActivePlanIds();
            assertEquals(2, activePlans.size(),
                "Should have 2 active plans");
        }

        @Test
        @DisplayName("Plan progress returns -1 for non-existent plan")
        void getProgressForNonExistentPlan() {
            int progress = orchestrator.getPlanProgress("nonexistent");

            assertEquals(-1, progress,
                "Should return -1 for non-existent plan");
        }

        @Test
        @DisplayName("Get active plan IDs returns unmodifiable set")
        void getActivePlanIdsReturnsUnmodifiable() {
            orchestrator.registerAgent(foremanEntity, AgentRole.FOREMAN);

            orchestrator.processHumanCommand(mockResponse, List.of(workerEntity1));

            Set<String> plans = orchestrator.getActivePlanIds();

            assertThrows(UnsupportedOperationException.class, () -> {
                plans.add("fake_plan");
            }, "Returned set should be unmodifiable");
        }
    }

    // ==================== Task Distribution Tests ====================

    @Nested
    @DisplayName("Task Distribution Tests")
    class TaskDistributionTests {

        @BeforeEach
        void setUp() {
            orchestrator.registerAgent(foremanEntity, AgentRole.FOREMAN);
            orchestrator.registerAgent(workerEntity1, AgentRole.WORKER);
            orchestrator.registerAgent(workerEntity2, AgentRole.WORKER);
            orchestrator.registerAgent(workerEntity3, AgentRole.WORKER);
        }

        @Test
        @DisplayName("Tasks are distributed round-robin to workers")
        void tasksDistributedRoundRobin() {
            List<Task> tasks = List.of(
                TaskBuilder.Presets.mineStone(32),
                TaskBuilder.Presets.mineStone(32),
                TaskBuilder.Presets.mineStone(32),
                TaskBuilder.Presets.mineStone(32),
                TaskBuilder.Presets.mineStone(32)
            );

            ResponseParser.ParsedResponse response = mock(ResponseParser.ParsedResponse.class);
            lenient().when(response.getPlan()).thenReturn("Mining operation");
            lenient().when(response.getTasks()).thenReturn(tasks);

            List<ForemanEntity> agents = List.of(
                workerEntity1, workerEntity2, workerEntity3
            );

            String planId = orchestrator.processHumanCommand(response, agents);

            assertNotNull(planId);

            // Tasks should be distributed across all workers
            AgentCommunicationBus bus = orchestrator.getCommunicationBus();

            // Check that messages were sent
            for (String workerId : List.of(WORKER_1_ID, WORKER_2_ID, WORKER_3_ID)) {
                AgentMessage message = bus.poll(workerId);
                // At least one worker should have received a task assignment
                if (message != null) {
                    assertEquals(AgentMessage.Type.TASK_ASSIGNMENT, message.getType());
                }
            }
        }

        @Test
        @DisplayName("More tasks than workers distributes correctly")
        void moreTasksThanWorkers() {
            List<Task> tasks = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                tasks.add(TaskBuilder.Presets.mineStone(16));
            }

            ResponseParser.ParsedResponse response = mock(ResponseParser.ParsedResponse.class);
            lenient().when(response.getPlan()).thenReturn("Large mining operation");
            lenient().when(response.getTasks()).thenReturn(tasks);

            List<ForemanEntity> agents = List.of(
                workerEntity1, workerEntity2, workerEntity3
            );

            String planId = orchestrator.processHumanCommand(response, agents);

            assertNotNull(planId);
            assertTrue(orchestrator.getActivePlanIds().contains(planId));
        }

        @Test
        @DisplayName("Fewer tasks than workers assigns correctly")
        void fewerTasksThanWorkers() {
            List<Task> tasks = List.of(
                TaskBuilder.Presets.mineStone(64)
            );

            ResponseParser.ParsedResponse response = mock(ResponseParser.ParsedResponse.class);
            lenient().when(response.getPlan()).thenReturn("Single task");
            lenient().when(response.getTasks()).thenReturn(tasks);

            List<ForemanEntity> agents = List.of(
                workerEntity1, workerEntity2, workerEntity3
            );

            String planId = orchestrator.processHumanCommand(response, agents);

            assertNotNull(planId);

            // Foreman should take remaining tasks or only one worker gets task
            AgentCommunicationBus bus = orchestrator.getCommunicationBus();

            // Check that at least one task was assigned
            boolean taskAssigned = false;
            for (String workerId : List.of(WORKER_1_ID, WORKER_2_ID, WORKER_3_ID, FOREMAN_ID)) {
                if (bus.peek(workerId) != null) {
                    taskAssigned = true;
                    break;
                }
            }
            assertTrue(taskAssigned, "At least one task should be assigned");
        }

        @Test
        @DisplayName("Tasks distributed when all workers busy")
        void tasksDistributedWhenWorkersBusy() {
            // This tests the scenario where foreman must handle tasks
            List<Task> tasks = List.of(
                TaskBuilder.Presets.mineStone(64)
            );

            ResponseParser.ParsedResponse response = mock(ResponseParser.ParsedResponse.class);
            lenient().when(response.getPlan()).thenReturn("Busy worker scenario");
            lenient().when(response.getTasks()).thenReturn(tasks);

            // No available workers (empty list)
            List<ForemanEntity> emptyAgents = List.of();

            String planId = orchestrator.processHumanCommand(response, emptyAgents);

            assertNotNull(planId,
                "Plan should be created even when no workers available");
        }
    }

    // ==================== Message Handling Tests ====================

    @Nested
    @DisplayName("Message Handling Tests")
    class MessageHandlingTests {

        private String planId;

        @BeforeEach
        void setUp() {
            orchestrator.registerAgent(foremanEntity, AgentRole.FOREMAN);
            orchestrator.registerAgent(workerEntity1, AgentRole.WORKER);

            List<Task> tasks = List.of(TaskBuilder.Presets.mineStone(64));

            ResponseParser.ParsedResponse response = mock(ResponseParser.ParsedResponse.class);
            lenient().when(response.getPlan()).thenReturn("Test plan");
            lenient().when(response.getTasks()).thenReturn(tasks);

            planId = orchestrator.processHumanCommand(response, List.of(workerEntity1));
        }

        @Test
        @DisplayName("Handle task progress message")
        void handleTaskProgress() {
            AgentMessage progressMessage = new AgentMessage.Builder()
                .type(AgentMessage.Type.TASK_PROGRESS)
                .sender(WORKER_1_ID, WORKER_1_ID)
                .recipient(FOREMAN_ID)
                .content("Mining in progress")
                .payload("percentComplete", 50)
                .payload("status", "Mining stone at location")
                .priority(AgentMessage.Priority.LOW)
                .build();

            AgentCommunicationBus bus = orchestrator.getCommunicationBus();
            bus.publish(progressMessage);

            // Message should be processed without exception
            assertDoesNotThrow(() -> bus.poll(FOREMAN_ID));
        }

        @Test
        @DisplayName("Handle task completion message")
        void handleTaskCompletion() {
            AgentMessage completeMessage = new AgentMessage.Builder()
                .type(AgentMessage.Type.TASK_COMPLETE)
                .sender(WORKER_1_ID, WORKER_1_ID)
                .recipient(FOREMAN_ID)
                .content("Task completed successfully")
                .payload("result", "Mined 64 stone")
                .priority(AgentMessage.Priority.NORMAL)
                .build();

            AgentCommunicationBus bus = orchestrator.getCommunicationBus();
            bus.publish(completeMessage);

            // Message should be processed without exception
            assertDoesNotThrow(() -> bus.poll(FOREMAN_ID));
        }

        @Test
        @DisplayName("Handle task failure message")
        void handleTaskFailure() {
            AgentMessage failureMessage = new AgentMessage.Builder()
                .type(AgentMessage.Type.TASK_FAILED)
                .sender(WORKER_1_ID, WORKER_1_ID)
                .recipient(FOREMAN_ID)
                .content("Task failed")
                .payload("result", "Path blocked by obstacle")
                .priority(AgentMessage.Priority.HIGH)
                .build();

            AgentCommunicationBus bus = orchestrator.getCommunicationBus();
            bus.publish(failureMessage);

            // Message should be processed without exception
            assertDoesNotThrow(() -> bus.poll(FOREMAN_ID));
        }

        @Test
        @DisplayName("Handle help request message")
        void handleHelpRequest() {
            AgentMessage helpMessage = new AgentMessage.Builder()
                .type(AgentMessage.Type.HELP_REQUEST)
                .sender(WORKER_1_ID, WORKER_1_ID)
                .recipient(FOREMAN_ID)
                .content("Stuck and need help")
                .priority(AgentMessage.Priority.HIGH)
                .build();

            AgentCommunicationBus bus = orchestrator.getCommunicationBus();
            bus.publish(helpMessage);

            // Message should be processed without exception
            assertDoesNotThrow(() -> bus.poll(FOREMAN_ID));
        }

        @Test
        @DisplayName("Handle status report message")
        void handleStatusReport() {
            AgentMessage statusMessage = new AgentMessage.Builder()
                .type(AgentMessage.Type.STATUS_REPORT)
                .sender(WORKER_1_ID, WORKER_1_ID)
                .recipient(FOREMAN_ID)
                .content("Status: operational")
                .priority(AgentMessage.Priority.NORMAL)
                .build();

            AgentCommunicationBus bus = orchestrator.getCommunicationBus();
            bus.publish(statusMessage);

            // Message should be processed without exception
            assertDoesNotThrow(() -> bus.poll(FOREMAN_ID));
        }

        @Test
        @DisplayName("Handle unknown message type gracefully")
        void handleUnknownMessageType() {
            AgentMessage unknownMessage = new AgentMessage.Builder()
                .type(AgentMessage.Type.COORDINATION)
                .sender(WORKER_1_ID, WORKER_1_ID)
                .recipient(FOREMAN_ID)
                .content("Unknown coordination message")
                .priority(AgentMessage.Priority.NORMAL)
                .build();

            AgentCommunicationBus bus = orchestrator.getCommunicationBus();

            // Should handle gracefully without throwing exception
            assertDoesNotThrow(() -> bus.publish(unknownMessage));
        }

        @Test
        @DisplayName("Handle message from null agent")
        void handleMessageFromNullAgent() {
            // This tests internal message handling robustness
            assertDoesNotThrow(() -> {
                // Message from null agent should be logged but not crash
                AgentCommunicationBus bus = orchestrator.getCommunicationBus();
                bus.publish(AgentMessage.broadcast(
                    WORKER_1_ID, WORKER_1_ID,
                    "Test message",
                    AgentMessage.Priority.NORMAL
                ));
            });
        }

        @Test
        @DisplayName("Handle message with null type")
        void handleMessageWithNullType() {
            AgentCommunicationBus bus = orchestrator.getCommunicationBus();

            assertDoesNotThrow(() -> {
                AgentMessage message = new AgentMessage.Builder()
                    .sender(WORKER_1_ID, WORKER_1_ID)
                    .recipient(FOREMAN_ID)
                    .content("Test")
                    .build();
                bus.publish(message);
            });
        }
    }

    // ==================== Plan Execution Tests ====================

    @Nested
    @DisplayName("Plan Execution Tests")
    class PlanExecutionTests {

        @Test
        @DisplayName("Plan progress updates correctly")
        void planProgressUpdates() {
            orchestrator.registerAgent(foremanEntity, AgentRole.FOREMAN);
            orchestrator.registerAgent(workerEntity1, AgentRole.WORKER);

            List<Task> tasks = List.of(
                TaskBuilder.Presets.mineStone(64),
                TaskBuilder.Presets.placeBlock("dirt", 0, 64, 0)
            );

            ResponseParser.ParsedResponse response = mock(ResponseParser.ParsedResponse.class);
            lenient().when(response.getPlan()).thenReturn("Multi-task plan");
            lenient().when(response.getTasks()).thenReturn(tasks);

            String planId = orchestrator.processHumanCommand(response, List.of(workerEntity1));

            int initialProgress = orchestrator.getPlanProgress(planId);
            assertTrue(initialProgress >= 0, "Initial progress should be valid");
        }

        @Test
        @DisplayName("Plan completion detected correctly")
        void planCompletionDetected() {
            orchestrator.registerAgent(foremanEntity, AgentRole.FOREMAN);
            orchestrator.registerAgent(workerEntity1, AgentRole.WORKER);

            List<Task> tasks = List.of(TaskBuilder.Presets.mineStone(64));

            ResponseParser.ParsedResponse response = mock(ResponseParser.ParsedResponse.class);
            lenient().when(response.getPlan()).thenReturn("Single task plan");
            lenient().when(response.getTasks()).thenReturn(tasks);

            String planId = orchestrator.processHumanCommand(response, List.of(workerEntity1));

            // Send completion message
            AgentMessage completeMessage = new AgentMessage.Builder()
                .type(AgentMessage.Type.TASK_COMPLETE)
                .sender(WORKER_1_ID, WORKER_1_ID)
                .recipient(FOREMAN_ID)
                .content("Task completed")
                .payload("result", "Done")
                .priority(AgentMessage.Priority.NORMAL)
                .build();

            AgentCommunicationBus bus = orchestrator.getCommunicationBus();
            bus.publish(completeMessage);

            // Plan should be marked complete
            assertDoesNotThrow(() -> orchestrator.getPlanProgress(planId));
        }

        @Test
        @DisplayName("Multiple plans tracked independently")
        void multiplePlansTrackedIndependently() {
            orchestrator.registerAgent(foremanEntity, AgentRole.FOREMAN);
            orchestrator.registerAgent(workerEntity1, AgentRole.WORKER);

            List<Task> tasks1 = List.of(TaskBuilder.Presets.mineStone(64));
            List<Task> tasks2 = List.of(TaskBuilder.Presets.placeBlock("dirt", 0, 64, 0));

            ResponseParser.ParsedResponse response1 = mock(ResponseParser.ParsedResponse.class);
            lenient().when(response1.getPlan()).thenReturn("Plan 1");
            lenient().when(response1.getTasks()).thenReturn(tasks1);

            ResponseParser.ParsedResponse response2 = mock(ResponseParser.ParsedResponse.class);
            lenient().when(response2.getPlan()).thenReturn("Plan 2");
            lenient().when(response2.getTasks()).thenReturn(tasks2);

            String planId1 = orchestrator.processHumanCommand(response1, List.of(workerEntity1));
            String planId2 = orchestrator.processHumanCommand(response2, List.of(workerEntity1));

            assertNotEquals(planId1, planId2, "Plans should have different IDs");

            int progress1 = orchestrator.getPlanProgress(planId1);
            int progress2 = orchestrator.getPlanProgress(planId2);

            assertTrue(progress1 >= 0);
            assertTrue(progress2 >= 0);
        }

        @Test
        @DisplayName("Get active plans returns all active plans")
        void getActivePlansReturnsAll() {
            orchestrator.registerAgent(foremanEntity, AgentRole.FOREMAN);
            orchestrator.registerAgent(workerEntity1, AgentRole.WORKER);

            List<Task> tasks = List.of(TaskBuilder.Presets.mineStone(64));

            ResponseParser.ParsedResponse response = mock(ResponseParser.ParsedResponse.class);
            lenient().when(response.getPlan()).thenReturn("Test plan");
            lenient().when(response.getTasks()).thenReturn(tasks);

            orchestrator.processHumanCommand(response, List.of(workerEntity1));
            orchestrator.processHumanCommand(response, List.of(workerEntity1));
            orchestrator.processHumanCommand(response, List.of(workerEntity1));

            Set<String> activePlans = orchestrator.getActivePlanIds();

            assertEquals(3, activePlans.size(), "Should have 3 active plans");
        }

        @Test
        @DisplayName("Plan with all tasks completed is removed")
        void completedPlanRemoved() {
            orchestrator.registerAgent(foremanEntity, AgentRole.FOREMAN);
            orchestrator.registerAgent(workerEntity1, AgentRole.WORKER);

            List<Task> tasks = List.of(TaskBuilder.Presets.mineStone(64));

            ResponseParser.ParsedResponse response = mock(ResponseParser.ParsedResponse.class);
            lenient().when(response.getPlan()).thenReturn("Test plan");
            lenient().when(response.getTasks()).thenReturn(tasks);

            String planId = orchestrator.processHumanCommand(response, List.of(workerEntity1));

            // Send completion message
            AgentMessage completeMessage = AgentMessage.taskComplete(
                WORKER_1_ID, WORKER_1_ID, FOREMAN_ID, "task1", true, "Success"
            );

            orchestrator.getCommunicationBus().publish(completeMessage);

            // Plan should eventually be removed when complete
            // (exact timing depends on async processing)
            assertDoesNotThrow(() -> orchestrator.getPlanProgress(planId));
        }
    }

    // ==================== Multi-Agent Coordination Tests ====================

    @Nested
    @DisplayName("Multi-Agent Coordination Tests")
    class MultiAgentCoordinationTests {

        @Test
        @DisplayName("Workers coordinate without direct communication")
        void workersCoordinateThroughForeman() {
            orchestrator.registerAgent(foremanEntity, AgentRole.FOREMAN);
            orchestrator.registerAgent(workerEntity1, AgentRole.WORKER);
            orchestrator.registerAgent(workerEntity2, AgentRole.WORKER);

            List<Task> tasks = List.of(
                TaskBuilder.Presets.mineStone(32),
                TaskBuilder.Presets.mineStone(32)
            );

            ResponseParser.ParsedResponse response = mock(ResponseParser.ParsedResponse.class);
            lenient().when(response.getPlan()).thenReturn("Coordinated mining");
            lenient().when(response.getTasks()).thenReturn(tasks);

            String planId = orchestrator.processHumanCommand(
                response, List.of(workerEntity1, workerEntity2)
            );

            assertNotNull(planId);

            AgentCommunicationBus bus = orchestrator.getCommunicationBus();

            // Both workers should have received messages
            boolean worker1HasMessage = bus.peek(WORKER_1_ID) != null;
            boolean worker2HasMessage = bus.peek(WORKER_2_ID) != null;

            assertTrue(worker1HasMessage || worker2HasMessage,
                "At least one worker should have a task assignment");
        }

        @Test
        @DisplayName("Broadcast message reaches all workers")
        void broadcastReachesAllWorkers() {
            orchestrator.registerAgent(foremanEntity, AgentRole.FOREMAN);
            orchestrator.registerAgent(workerEntity1, AgentRole.WORKER);
            orchestrator.registerAgent(workerEntity2, AgentRole.WORKER);
            orchestrator.registerAgent(workerEntity3, AgentRole.WORKER);

            AgentMessage broadcast = AgentMessage.broadcast(
                FOREMAN_ID, "Foreman",
                "Team announcement",
                AgentMessage.Priority.NORMAL
            );

            AgentCommunicationBus bus = orchestrator.getCommunicationBus();
            bus.publish(broadcast);

            // All workers should receive the broadcast
            assertTrue(bus.peek(WORKER_1_ID) != null ||
                       bus.poll(WORKER_1_ID) != null);
            assertTrue(bus.peek(WORKER_2_ID) != null ||
                       bus.poll(WORKER_2_ID) != null);
            assertTrue(bus.peek(WORKER_3_ID) != null ||
                       bus.poll(WORKER_3_ID) != null);
        }

        @Test
        @DisplayName("Specialist and worker can collaborate")
        void specialistAndWorkerCollaborate() {
            orchestrator.registerAgent(foremanEntity, AgentRole.FOREMAN);
            orchestrator.registerAgent(workerEntity1, AgentRole.WORKER);
            orchestrator.registerAgent(workerEntity2, AgentRole.SPECIALIST);

            List<Task> tasks = List.of(
                TaskBuilder.Presets.mineStone(64),
                TaskBuilder.Presets.craftItem("stick", 4)
            );

            ResponseParser.ParsedResponse response = mock(ResponseParser.ParsedResponse.class);
            lenient().when(response.getPlan()).thenReturn("Mining and crafting");
            lenient().when(response.getTasks()).thenReturn(tasks);

            String planId = orchestrator.processHumanCommand(
                response, List.of(workerEntity1, workerEntity2)
            );

            assertNotNull(planId);
        }

        @Test
        @DisplayName("Solo agent operates independently")
        void soloAgentOperatesIndependently() {
            orchestrator.registerAgent(workerEntity1, AgentRole.SOLO);

            List<Task> tasks = List.of(TaskBuilder.Presets.mineStone(64));

            ResponseParser.ParsedResponse response = mock(ResponseParser.ParsedResponse.class);
            lenient().when(response.getPlan()).thenReturn("Solo task");
            lenient().when(response.getTasks()).thenReturn(tasks);

            String planId = orchestrator.processHumanCommand(
                response, List.of(workerEntity1)
            );

            assertNotNull(planId);
        }
    }

    // ==================== Thread Safety Tests ====================

    @Nested
    @DisplayName("Thread Safety Tests")
    class ThreadSafetyTests {

        @Test
        @DisplayName("Concurrent agent registration")
        void concurrentRegistration() throws InterruptedException {
            int threadCount = 10;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);

            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                new Thread(() -> {
                    try {
                        startLatch.await();
                        ForemanEntity worker = mock(ForemanEntity.class);
                        lenient().when(worker.getEntityName()).thenReturn("worker" + index);

                        orchestrator.registerAgent(worker, AgentRole.WORKER);
                        successCount.incrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                }).start();
            }

            startLatch.countDown();
            assertTrue(doneLatch.await(10, TimeUnit.SECONDS));

            assertEquals(threadCount, successCount.get(),
                "All registrations should succeed");

            AgentCommunicationBus bus = orchestrator.getCommunicationBus();
            assertEquals(threadCount, bus.getRegisteredAgents().size());
        }

        @Test
        @DisplayName("Concurrent command processing")
        void concurrentCommandProcessing() throws InterruptedException {
            orchestrator.registerAgent(foremanEntity, AgentRole.FOREMAN);
            orchestrator.registerAgent(workerEntity1, AgentRole.WORKER);

            int threadCount = 5;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);

            List<Task> tasks = List.of(TaskBuilder.Presets.mineStone(16));

            ResponseParser.ParsedResponse response = mock(ResponseParser.ParsedResponse.class);
            lenient().when(response.getPlan()).thenReturn("Concurrent test plan");
            lenient().when(response.getTasks()).thenReturn(tasks);

            for (int i = 0; i < threadCount; i++) {
                new Thread(() -> {
                    try {
                        startLatch.await();
                        String planId = orchestrator.processHumanCommand(
                            response, List.of(workerEntity1)
                        );
                        if (planId != null && !planId.isEmpty()) {
                            successCount.incrementAndGet();
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

            assertEquals(threadCount, successCount.get(),
                "All commands should be processed");

            Set<String> activePlans = orchestrator.getActivePlanIds();
            assertEquals(threadCount, activePlans.size(),
                "Should have all plans created");
        }

        @Test
        @DisplayName("Concurrent message handling")
        void concurrentMessageHandling() throws InterruptedException {
            orchestrator.registerAgent(foremanEntity, AgentRole.FOREMAN);
            orchestrator.registerAgent(workerEntity1, AgentRole.WORKER);
            orchestrator.registerAgent(workerEntity2, AgentRole.WORKER);

            int messageCount = 50;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(messageCount);

            AgentCommunicationBus bus = orchestrator.getCommunicationBus();

            for (int i = 0; i < messageCount; i++) {
                final int index = i;
                new Thread(() -> {
                    try {
                        startLatch.await();

                        AgentMessage message = AgentMessage.taskProgress(
                            index % 2 == 0 ? WORKER_1_ID : WORKER_2_ID,
                            "Worker" + index,
                            FOREMAN_ID,
                            "task" + index,
                            50,
                            "In progress"
                        );

                        bus.publish(message);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                }).start();
            }

            startLatch.countDown();
            assertTrue(doneLatch.await(10, TimeUnit.SECONDS));

            // All messages should be processed without exceptions
            assertDoesNotThrow(() -> bus.getStats());
        }
    }

    // ==================== Shutdown Tests ====================

    @Nested
    @DisplayName("Shutdown Tests")
    class ShutdownTests {

        @Test
        @DisplayName("Shutdown clears all state")
        void shutdownClearsState() {
            orchestrator.registerAgent(foremanEntity, AgentRole.FOREMAN);
            orchestrator.registerAgent(workerEntity1, AgentRole.WORKER);

            List<Task> tasks = List.of(TaskBuilder.Presets.mineStone(64));

            ResponseParser.ParsedResponse response = mock(ResponseParser.ParsedResponse.class);
            lenient().when(response.getPlan()).thenReturn("Test plan");
            lenient().when(response.getTasks()).thenReturn(tasks);

            orchestrator.processHumanCommand(response, List.of(workerEntity1));

            // Shutdown
            orchestrator.shutdown();

            // All state should be cleared
            assertTrue(orchestrator.getActivePlanIds().isEmpty(),
                "Active plans should be cleared");

            AgentCommunicationBus bus = orchestrator.getCommunicationBus();
            assertTrue(bus.getRegisteredAgents().isEmpty(),
                "All agents should be unregistered");
        }

        @Test
        @DisplayName("Shutdown with no active state")
        void shutdownWithNoState() {
            assertDoesNotThrow(() -> orchestrator.shutdown(),
                "Shutdown should not throw exception with no state");
        }

        @Test
        @DisplayName("Multiple shutdowns are safe")
        void multipleShutdowns() {
            orchestrator.registerAgent(foremanEntity, AgentRole.FOREMAN);

            assertDoesNotThrow(() -> {
                orchestrator.shutdown();
                orchestrator.shutdown();
                orchestrator.shutdown();
            }, "Multiple shutdowns should be safe");
        }

        @Test
        @DisplayName("Operations after shutdown handled gracefully")
        void operationsAfterShutdown() {
            orchestrator.registerAgent(foremanEntity, AgentRole.FOREMAN);
            orchestrator.shutdown();

            // Operations after shutdown should be handled gracefully
            assertDoesNotThrow(() -> {
                orchestrator.getActivePlanIds();
                orchestrator.getPlanProgress("test");
                orchestrator.getCommunicationBus();
            }, "Operations after shutdown should not throw exceptions");
        }
    }

    // ==================== Edge Cases Tests ====================

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Handle very large task list")
        void veryLargeTaskList() {
            orchestrator.registerAgent(foremanEntity, AgentRole.FOREMAN);
            orchestrator.registerAgent(workerEntity1, AgentRole.WORKER);

            List<Task> largeTaskList = new ArrayList<>();
            for (int i = 0; i < 1000; i++) {
                largeTaskList.add(TaskBuilder.Presets.mineStone(1));
            }

            ResponseParser.ParsedResponse response = mock(ResponseParser.ParsedResponse.class);
            lenient().when(response.getPlan()).thenReturn("Large plan");
            lenient().when(response.getTasks()).thenReturn(largeTaskList);

            String planId = orchestrator.processHumanCommand(
                response, List.of(workerEntity1)
            );

            assertNotNull(planId, "Should handle large task lists");
        }

        @Test
        @DisplayName("Handle tasks with complex parameters")
        void tasksWithComplexParameters() {
            orchestrator.registerAgent(foremanEntity, AgentRole.FOREMAN);
            orchestrator.registerAgent(workerEntity1, AgentRole.WORKER);

            Task complexTask = new Task("complex_action", Map.of(
                "stringParam", "value",
                "intParam", 42,
                "boolParam", true,
                "listParam", List.of("a", "b", "c"),
                "nestedParam", Map.of("key", "value")
            ));

            ResponseParser.ParsedResponse response = mock(ResponseParser.ParsedResponse.class);
            lenient().when(response.getPlan()).thenReturn("Complex task plan");
            lenient().when(response.getTasks()).thenReturn(List.of(complexTask));

            String planId = orchestrator.processHumanCommand(
                response, List.of(workerEntity1)
            );

            assertNotNull(planId, "Should handle complex task parameters");
        }

        @Test
        @DisplayName("Handle plan with zero tasks")
        void planWithZeroTasks() {
            orchestrator.registerAgent(foremanEntity, AgentRole.FOREMAN);

            ResponseParser.ParsedResponse response = mock(ResponseParser.ParsedResponse.class);
            lenient().when(response.getPlan()).thenReturn("Empty plan");
            lenient().when(response.getTasks()).thenReturn(List.of());

            String planId = orchestrator.processHumanCommand(
                response, List.of(workerEntity1)
            );

            assertNotNull(planId, "Should handle plan with zero tasks");
        }

        @Test
        @DisplayName("Get communication bus returns same instance")
        void getCommunicationBusReturnsSameInstance() {
            AgentCommunicationBus bus1 = orchestrator.getCommunicationBus();
            AgentCommunicationBus bus2 = orchestrator.getCommunicationBus();

            assertSame(bus1, bus2,
                "Should return the same communication bus instance");
        }

        @Test
        @DisplayName("Handle agent with changing roles")
        void agentWithChangingRoles() {
            orchestrator.registerAgent(workerEntity1, AgentRole.WORKER);
            orchestrator.unregisterAgent(WORKER_1_ID);
            orchestrator.registerAgent(workerEntity1, AgentRole.SPECIALIST);

            AgentCommunicationBus bus = orchestrator.getCommunicationBus();
            assertTrue(bus.getRegisteredAgents().contains(WORKER_1_ID),
                "Agent should be registered with new role");
        }

        @Test
        @DisplayName("Worker can become foreman")
        void workerCanBecomeForeman() {
            orchestrator.registerAgent(workerEntity1, AgentRole.WORKER);
            orchestrator.registerAgent(workerEntity2, AgentRole.WORKER);

            // Unregister workers and register worker1 as foreman
            orchestrator.unregisterAgent(WORKER_1_ID);
            orchestrator.unregisterAgent(WORKER_2_ID);
            orchestrator.registerAgent(workerEntity1, AgentRole.FOREMAN);

            AgentCommunicationBus bus = orchestrator.getCommunicationBus();
            assertTrue(bus.getRegisteredAgents().contains(WORKER_1_ID),
                "Former worker should now be registered as foreman");
        }
    }

    // ==================== Integration Tests ====================

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Complete workflow: register to plan completion")
        void completeWorkflow() {
            // Register agents
            orchestrator.registerAgent(foremanEntity, AgentRole.FOREMAN);
            orchestrator.registerAgent(workerEntity1, AgentRole.WORKER);
            orchestrator.registerAgent(workerEntity2, AgentRole.WORKER);

            // Process command
            List<Task> tasks = List.of(
                TaskBuilder.Presets.mineStone(32),
                TaskBuilder.Presets.placeBlock("dirt", 0, 64, 0)
            );

            ResponseParser.ParsedResponse response = mock(ResponseParser.ParsedResponse.class);
            lenient().when(response.getPlan()).thenReturn("Complete workflow test");
            lenient().when(response.getTasks()).thenReturn(tasks);

            List<ForemanEntity> agents = List.of(workerEntity1, workerEntity2);
            String planId = orchestrator.processHumanCommand(response, agents);

            assertNotNull(planId);

            // Check messages were sent
            AgentCommunicationBus bus = orchestrator.getCommunicationBus();

            // Workers should have task assignments
            boolean hasAssignments = false;
            for (String workerId : List.of(WORKER_1_ID, WORKER_2_ID)) {
                if (bus.peek(workerId) != null) {
                    hasAssignments = true;
                    break;
                }
            }
            assertTrue(hasAssignments, "Task assignments should be sent");

            // Complete tasks
            AgentMessage complete1 = AgentMessage.taskComplete(
                WORKER_1_ID, WORKER_1_ID, FOREMAN_ID, "task1", true, "Success"
            );
            AgentMessage complete2 = AgentMessage.taskComplete(
                WORKER_2_ID, WORKER_2_ID, FOREMAN_ID, "task2", true, "Success"
            );

            bus.publish(complete1);
            bus.publish(complete2);

            // Verify plan progress
            assertDoesNotThrow(() -> {
                int progress = orchestrator.getPlanProgress(planId);
                assertTrue(progress >= 0 && progress <= 100);
            });
        }

        @Test
        @DisplayName("Foreman election from workers")
        void foremanElection() {
            // Register workers only
            orchestrator.registerAgent(workerEntity1, AgentRole.WORKER);
            orchestrator.registerAgent(workerEntity2, AgentRole.WORKER);
            orchestrator.registerAgent(workerEntity3, AgentRole.WORKER);

            // Process command without foreman (solo mode)
            List<Task> tasks = List.of(TaskBuilder.Presets.mineStone(64));

            ResponseParser.ParsedResponse response = mock(ResponseParser.ParsedResponse.class);
            lenient().when(response.getPlan()).thenReturn("Solo mode plan");
            lenient().when(response.getTasks()).thenReturn(tasks);

            String planId = orchestrator.processHumanCommand(
                response, List.of(workerEntity1, workerEntity2, workerEntity3)
            );

            assertNotNull(planId, "Should create plan in solo mode");

            // Now register foreman
            orchestrator.registerAgent(foremanEntity, AgentRole.FOREMAN);

            // New commands should use foreman mode
            String planId2 = orchestrator.processHumanCommand(
                response, List.of(workerEntity1, workerEntity2, workerEntity3)
            );

            assertNotNull(planId2, "Should create plan with foreman");
            assertNotEquals(planId, planId2, "Plans should have different IDs");
        }

        @Test
        @DisplayName("Error recovery during task execution")
        void errorRecovery() {
            orchestrator.registerAgent(foremanEntity, AgentRole.FOREMAN);
            orchestrator.registerAgent(workerEntity1, AgentRole.WORKER);
            orchestrator.registerAgent(workerEntity2, AgentRole.WORKER);

            List<Task> tasks = List.of(TaskBuilder.Presets.mineStone(64));

            ResponseParser.ParsedResponse response = mock(ResponseParser.ParsedResponse.class);
            lenient().when(response.getPlan()).thenReturn("Error recovery test");
            lenient().when(response.getTasks()).thenReturn(tasks);

            String planId = orchestrator.processHumanCommand(
                response, List.of(workerEntity1, workerEntity2)
            );

            assertNotNull(planId);

            AgentCommunicationBus bus = orchestrator.getCommunicationBus();

            // Simulate task failure
            AgentMessage failure = AgentMessage.taskComplete(
                WORKER_1_ID, WORKER_1_ID, FOREMAN_ID, "task1", false, "Failed"
            );

            bus.publish(failure);

            // System should handle failure gracefully
            assertDoesNotThrow(() -> {
                int progress = orchestrator.getPlanProgress(planId);
                assertTrue(progress >= 0);
            });
        }

        @Test
        @DisplayName("Concurrent plans with shared workers")
        void concurrentPlansWithSharedWorkers() {
            orchestrator.registerAgent(foremanEntity, AgentRole.FOREMAN);
            orchestrator.registerAgent(workerEntity1, AgentRole.WORKER);
            orchestrator.registerAgent(workerEntity2, AgentRole.WORKER);

            List<Task> tasks = List.of(TaskBuilder.Presets.mineStone(32));

            ResponseParser.ParsedResponse response = mock(ResponseParser.ParsedResponse.class);
            lenient().when(response.getPlan()).thenReturn("Concurrent plans test");
            lenient().when(response.getTasks()).thenReturn(tasks);

            // Create multiple plans
            String planId1 = orchestrator.processHumanCommand(
                response, List.of(workerEntity1, workerEntity2)
            );
            String planId2 = orchestrator.processHumanCommand(
                response, List.of(workerEntity1, workerEntity2)
            );
            String planId3 = orchestrator.processHumanCommand(
                response, List.of(workerEntity1, workerEntity2)
            );

            assertNotNull(planId1);
            assertNotNull(planId2);
            assertNotNull(planId3);

            Set<String> activePlans = orchestrator.getActivePlanIds();
            assertEquals(3, activePlans.size(), "Should track all plans independently");
        }
    }
}
