package com.minewright.coordination;

import com.minewright.action.Task;
import com.minewright.testutil.TestLogger;
import com.minewright.orchestration.AgentMessage;
import com.minewright.orchestration.AgentRole;

import org.slf4j.Logger;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * High-level coordinator for multi-agent task allocation using Contract Net Protocol.
 *
 * <p><b>Responsibilities:</b></p>
 * <ul>
 *   <li>Decompose complex tasks into subtasks</li>
 *   <li>Coordinate Contract Net bidding for subtasks</li>
 *   <li>Monitor task progress and handle failures</li>
 *   <li>Rebalance workload when agents become idle</li>
 * </ul>
 *
 * <p><b>Coordination Flow:</b></p>
 * <pre>
 * Complex Task Received
 *   |
 *   v
 * Decompose into Subtasks
 *   |
 *   v
 * For each subtask:
 *   |-- Announce via Contract Net
 *   |-- Collect bids
 *   |-- Award to best agent
 *   |
 *   v
 * Monitor Progress
 *   |-- Handle failures
 *   |-- Rebalance on idle
 *   |
 *   v
 * Report Completion
 * </pre>
 *
 * @see ContractNetManager
 * @see CapabilityRegistry
 * @since 1.3.0
 */
public class MultiAgentCoordinator {
    /**
     * Logger for this class.
     * Uses TestLogger to avoid triggering MineWrightMod initialization during tests.
     */
    private static final Logger LOGGER = TestLogger.getLogger(MultiAgentCoordinator.class);

    /**
     * State of a coordinated task.
     */
    public enum TaskState {
        /** Task being decomposed */
        DECOMPOSING,
        /** Subtasks announced, collecting bids */
        BIDDING,
        /** Contracts awarded, executing */
        EXECUTING,
        /** All subtasks completed */
        COMPLETED,
        /** Task failed */
        FAILED
    }

    /**
     * Tracks a coordinated multi-agent task.
     */
    public static class CoordinatedTask {
        private final String taskId;
        private final String description;
        private final Task complexTask;
        private final List<SubTaskAllocation> subtasks;
        private final TaskState state;
        private final UUID requesterId;
        private final long createdTime;
        private final long completionTime;

        public CoordinatedTask(String taskId, String description, Task complexTask, UUID requesterId) {
            this(taskId, description, complexTask, requesterId, new ArrayList<>(),
                 TaskState.DECOMPOSING, System.currentTimeMillis(), 0);
        }

        private CoordinatedTask(
            String taskId,
            String description,
            Task complexTask,
            UUID requesterId,
            List<SubTaskAllocation> subtasks,
            TaskState state,
            long createdTime,
            long completionTime
        ) {
            this.taskId = taskId;
            this.description = description;
            this.complexTask = complexTask;
            this.requesterId = requesterId;
            this.subtasks = subtasks;
            this.state = state;
            this.createdTime = createdTime;
            this.completionTime = completionTime;
        }

        public String getTaskId() {
            return taskId;
        }

        public String getDescription() {
            return description;
        }

        public Task getComplexTask() {
            return complexTask;
        }

        public List<SubTaskAllocation> getSubtasks() {
            return Collections.unmodifiableList(subtasks);
        }

        public TaskState getState() {
            return state;
        }

        public UUID getRequesterId() {
            return requesterId;
        }

        public long getCreatedTime() {
            return createdTime;
        }

        public long getCompletionTime() {
            return completionTime;
        }

        public boolean isComplete() {
            return state == TaskState.COMPLETED || state == TaskState.FAILED;
        }

        public double getProgress() {
            if (subtasks.isEmpty()) {
                return 0.0;
            }
            long completed = subtasks.stream().filter(SubTaskAllocation::isComplete).count();
            return (double) completed / subtasks.size();
        }

        public CoordinatedTask withState(TaskState newState) {
            return new CoordinatedTask(taskId, description, complexTask, requesterId,
                subtasks, newState, createdTime,
                newState == TaskState.COMPLETED || newState == TaskState.FAILED
                    ? System.currentTimeMillis() : completionTime);
        }

        public CoordinatedTask withSubtask(SubTaskAllocation subtask) {
            List<SubTaskAllocation> newSubtasks = new ArrayList<>(subtasks);
            newSubtasks.add(subtask);
            return new CoordinatedTask(taskId, description, complexTask, requesterId,
                newSubtasks, state, createdTime, completionTime);
        }
    }

    /**
     * Tracks allocation of a single subtask.
     */
    public static class SubTaskAllocation {
        private final String subtaskId;
        private final Task subtask;
        private final UUID assignedAgent;
        private final String announcementId;
        private final boolean complete;
        private final String result;

        public SubTaskAllocation(String subtaskId, Task subtask, UUID assignedAgent,
                                String announcementId) {
            this(subtaskId, subtask, assignedAgent, announcementId, false, null);
        }

        private SubTaskAllocation(String subtaskId, Task subtask, UUID assignedAgent,
                                 String announcementId, boolean complete, String result) {
            this.subtaskId = subtaskId;
            this.subtask = subtask;
            this.assignedAgent = assignedAgent;
            this.announcementId = announcementId;
            this.complete = complete;
            this.result = result;
        }

        public String getSubtaskId() {
            return subtaskId;
        }

        public Task getSubtask() {
            return subtask;
        }

        public UUID getAssignedAgent() {
            return assignedAgent;
        }

        public String getAnnouncementId() {
            return announcementId;
        }

        public boolean isComplete() {
            return complete;
        }

        public String getResult() {
            return result;
        }

        public SubTaskAllocation markComplete(String result) {
            return new SubTaskAllocation(subtaskId, subtask, assignedAgent,
                announcementId, true, result);
        }
    }

    /**
     * Strategy for decomposing complex tasks.
     */
    @FunctionalInterface
    public interface TaskDecomposer {
        /**
         * Decomposes a complex task into subtasks.
         *
         * @param complexTask The task to decompose
         * @return List of subtasks
         */
        List<Task> decompose(Task complexTask);
    }

    private final ContractNetManager contractNet;
    private final CapabilityRegistry capabilityRegistry;
    private final Map<String, CoordinatedTask> activeTasks;
    private final Map<UUID, String> agentToTaskMap;
    private TaskDecomposer taskDecomposer;
    private final ScheduledExecutorService scheduler;
    private final Map<UUID, String> agentNameCache;

    /**
     * Creates a new multi-agent coordinator.
     *
     * @param contractNet Contract Net manager for bidding
     * @param capabilityRegistry Capability registry for agent info
     */
    public MultiAgentCoordinator(
        ContractNetManager contractNet,
        CapabilityRegistry capabilityRegistry
    ) {
        this.contractNet = contractNet;
        this.capabilityRegistry = capabilityRegistry;
        this.activeTasks = new ConcurrentHashMap<>();
        this.agentToTaskMap = new ConcurrentHashMap<>();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "MultiAgentCoordinator");
            t.setDaemon(true);
            return t;
        });
        this.agentNameCache = new ConcurrentHashMap<>();

        // Setup periodic monitoring
        scheduler.scheduleAtFixedRate(this::monitorProgress, 5, 5, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(this::cleanup, 30, 30, TimeUnit.SECONDS);

        // Setup contract net listeners
        contractNet.addListener(new ContractNetManager.ContractListener() {
            @Override
            public void onContractAwarded(String announcementId, TaskBid winner) {
                handleContractAwarded(announcementId, winner);
            }
        });
    }

    /**
     * Sets the task decomposer strategy.
     *
     * @param decomposer The decomposer to use
     */
    public void setTaskDecomposer(TaskDecomposer decomposer) {
        this.taskDecomposer = decomposer;
    }

    /**
     * Gets the task decomposer.
     *
     * @return Current decomposer, or default if not set
     */
    public TaskDecomposer getTaskDecomposer() {
        if (taskDecomposer == null) {
            // Default decomposer - simple action-based decomposition
            return task -> {
                // For now, just return the task as-is
                // A real implementation would use LLM to decompose
                return Collections.singletonList(task);
            };
        }
        return taskDecomposer;
    }

    /**
     * Coordinates execution of a complex task across multiple agents.
     *
     * @param complexTask The task to coordinate
     * @param requester Agent requesting the task
     * @return Future for the coordinated task
     */
    public CompletableFuture<CoordinatedTask> coordinateTask(Task complexTask, UUID requester) {
        String taskId = "coord_" + UUID.randomUUID().toString().substring(0, 8);
        CompletableFuture<CoordinatedTask> future = new CompletableFuture<>();

        CoordinatedTask coordTask = new CoordinatedTask(taskId, complexTask.toString(),
            complexTask, requester);
        activeTasks.put(taskId, coordTask);

        LOGGER.info("Coordinating complex task: {} ({})", taskId, complexTask.getAction());

        // Decompose task asynchronously
        CompletableFuture.supplyAsync(() -> {
            try {
                List<Task> subtasks = getTaskDecomposer().decompose(complexTask);
                LOGGER.info("Decomposed task {} into {} subtasks", taskId, subtasks.size());
                return subtasks;
            } catch (Exception e) {
                LOGGER.error("Task decomposition failed for {}", taskId, e);
                throw new CompletionException(e);
            }
        }).thenAcceptAsync(subtasks -> {
            // Announce each subtask for bidding
            announceSubtasks(taskId, subtasks, requester);
        }).exceptionally(ex -> {
            CoordinatedTask failed = activeTasks.get(taskId).withState(TaskState.FAILED);
            activeTasks.put(taskId, failed);
            future.completeExceptionally(ex);
            return null;
        });

        return future;
    }

    /**
     * Announces subtasks for bidding.
     */
    private void announceSubtasks(String taskId, List<Task> subtasks, UUID requester) {
        List<String> announcementIds = new ArrayList<>();

        for (int i = 0; i < subtasks.size(); i++) {
            Task subtask = subtasks.get(i);
            String subtaskId = taskId + "_sub" + i;

            TaskAnnouncement announcement = TaskAnnouncement.builder()
                .task(subtask)
                .requesterId(requester)
                .deadlineAfter(30000) // 30 second bidding window
                .build();

            contractNet.announceTask(subtask, requester, 30000);
            announcementIds.add(announcement.announcementId());

            LOGGER.debug("Announced subtask {} for bidding", subtaskId);
        }

        // Wait for bids and award contracts
        scheduler.schedule(() -> {
            awardContracts(taskId, announcementIds);
        }, 35, TimeUnit.SECONDS);
    }

    /**
     * Awards contracts for subtasks.
     */
    private void awardContracts(String taskId, List<String> announcementIds) {
        CoordinatedTask coordTask = activeTasks.get(taskId);
        if (coordTask == null || coordTask.getState() != TaskState.DECOMPOSING) {
            return;
        }

        int awarded = 0;

        for (String announcementId : announcementIds) {
            Optional<TaskBid> winner = contractNet.awardToBestBidder(announcementId);

            if (winner.isPresent()) {
                TaskBid bid = winner.get();
                String subtaskId = taskId + "_sub" + awarded;

                SubTaskAllocation allocation = new SubTaskAllocation(
                    subtaskId,
                    contractNet.getNegotiation(announcementId).getAnnouncement().task(),
                    bid.bidderId(),
                    announcementId
                );

                // Send task assignment to winner
                sendTaskAssignment(bid.bidderId(), allocation.getSubtask(), taskId);

                awarded++;
            } else {
                LOGGER.warn("No bids received for announcement {}", announcementId);
            }
        }

        CoordinatedTask updated = coordTask.withState(
            awarded > 0 ? TaskState.EXECUTING : TaskState.FAILED
        );
        activeTasks.put(taskId, updated);

        LOGGER.info("Awarded {} contracts for task {}", awarded, taskId);
    }

    /**
     * Handles a contract being awarded.
     */
    private void handleContractAwarded(String announcementId, TaskBid winner) {
        LOGGER.info("Contract awarded: {} to agent {}",
            announcementId, winner.bidderId().toString().substring(0, 8));

        // Update agent-to-task mapping
        // Find the parent task for this announcement
        for (CoordinatedTask coordTask : activeTasks.values()) {
            for (SubTaskAllocation subtask : coordTask.getSubtasks()) {
                if (subtask.getAnnouncementId().equals(announcementId)) {
                    agentToTaskMap.put(winner.bidderId(), coordTask.getTaskId());
                    return;
                }
            }
        }
    }

    /**
     * Sends a task assignment to an agent.
     */
    private void sendTaskAssignment(UUID agentId, Task task, String parentTaskId) {
        // This would integrate with the orchestration system
        LOGGER.info("Sending task assignment to agent {}: {} (parent: {})",
            agentId.toString().substring(0, 8), task.getAction(), parentTaskId);
        // TODO: Send via AgentCommunicationBus
    }

    /**
     * Monitors progress of active coordinated tasks.
     */
    private void monitorProgress() {
        for (CoordinatedTask coordTask : activeTasks.values()) {
            if (coordTask.getState() == TaskState.EXECUTING) {
                double progress = coordTask.getProgress();

                if (progress >= 1.0) {
                    // All subtasks complete
                    CoordinatedTask completed = coordTask.withState(TaskState.COMPLETED);
                    activeTasks.put(coordTask.getTaskId(), completed);

                    LOGGER.info("Coordinated task {} completed", coordTask.getTaskId());

                    // Clean up agent mappings
                    for (SubTaskAllocation subtask : coordTask.getSubtasks()) {
                        agentToTaskMap.remove(subtask.getAssignedAgent());
                    }
                }
            }
        }
    }

    /**
     * Rebalances workload when an agent becomes idle.
     *
     * @param agentId The idle agent
     */
    public void rebalanceOnAgentIdle(UUID agentId) {
        String currentTaskId = agentToTaskMap.get(agentId);

        if (currentTaskId != null) {
            CoordinatedTask coordTask = activeTasks.get(currentTaskId);

            if (coordTask != null && coordTask.getState() == TaskState.EXECUTING) {
                // Check if there are incomplete subtasks that could be reassigned
                for (SubTaskAllocation subtask : coordTask.getSubtasks()) {
                    if (!subtask.isComplete()) {
                        // Could potentially reassign this subtask
                        LOGGER.debug("Could reassign subtask {} to idle agent {}",
                            subtask.getSubtaskId(), agentId.toString().substring(0, 8));
                    }
                }
            }
        }

        // Remove from task mapping
        agentToTaskMap.remove(agentId);
    }

    /**
     * Handles a task completion report from an agent.
     *
     * @param agentId The reporting agent
     * @param announcementId The original announcement ID
     * @param success Whether the task succeeded
     * @param result Completion result message
     */
    public void handleTaskReport(UUID agentId, String announcementId, boolean success, String result) {
        // Find the subtask allocation
        for (CoordinatedTask coordTask : activeTasks.values()) {
            for (int i = 0; i < coordTask.getSubtasks().size(); i++) {
                SubTaskAllocation subtask = coordTask.getSubtasks().get(i);

                if (subtask.getAnnouncementId().equals(announcementId)) {
                    if (success) {
                        // Mark subtask as complete
                        List<SubTaskAllocation> newSubtasks = new ArrayList<>(coordTask.getSubtasks());
                        newSubtasks.set(i, subtask.markComplete(result));

                        CoordinatedTask updated = new CoordinatedTask(
                            coordTask.getTaskId(),
                            coordTask.getDescription(),
                            coordTask.getComplexTask(),
                            coordTask.getRequesterId(),
                            newSubtasks,
                            coordTask.getState(),
                            coordTask.getCreatedTime(),
                            coordTask.getCompletionTime()
                        );
                        activeTasks.put(coordTask.getTaskId(), updated);

                        LOGGER.info("Subtask {} completed: {}",
                            subtask.getSubtaskId(), result);
                    } else {
                        LOGGER.warn("Subtask {} failed: {}",
                            subtask.getSubtaskId(), result);
                        // Could trigger retry or reassignment here
                    }
                    return;
                }
            }
        }
    }

    /**
     * Gets an active coordinated task by ID.
     *
     * @param taskId Task ID
     * @return The task, or null if not found
     */
    public CoordinatedTask getTask(String taskId) {
        return activeTasks.get(taskId);
    }

    /**
     * Gets all active coordinated tasks.
     *
     * @return Unmodifiable collection of tasks
     */
    public Collection<CoordinatedTask> getActiveTasks() {
        return Collections.unmodifiableCollection(activeTasks.values());
    }

    /**
     * Cleans up completed/failed tasks.
     */
    private void cleanup() {
        long now = System.currentTimeMillis();
        int cleaned = 0;

        Iterator<Map.Entry<String, CoordinatedTask>> it = activeTasks.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, CoordinatedTask> entry = it.next();
            CoordinatedTask task = entry.getValue();

            boolean shouldRemove = false;

            if (task.isComplete()) {
                // Remove completed tasks after 5 minutes
                long age = now - task.getCompletionTime();
                shouldRemove = age > 300000;
            } else if (task.getState() == TaskState.DECOMPOSING) {
                // Remove tasks stuck in decomposition for too long
                long age = now - task.getCreatedTime();
                shouldRemove = age > 60000; // 1 minute
            }

            if (shouldRemove) {
                it.remove();
                cleaned++;
            }
        }

        if (cleaned > 0) {
            LOGGER.debug("Cleaned up {} coordinated tasks", cleaned);
        }
    }

    /**
     * Shuts down the coordinator.
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
