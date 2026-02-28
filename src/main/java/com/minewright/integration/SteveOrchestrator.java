package com.minewright.integration;

import com.minewright.action.Task;
import com.minewright.testutil.TestLogger;
import org.slf4j.Logger;
import com.minewright.blackboard.Blackboard;
import com.minewright.blackboard.BlackboardEntry;
import com.minewright.blackboard.KnowledgeArea;
import com.minewright.communication.AgentMessage;
import com.minewright.communication.CommunicationBus;
import com.minewright.decision.DecisionContext;
import com.minewright.decision.TaskPrioritizer;
import com.minewright.entity.ForemanEntity;
import com.minewright.llm.ResponseParser;
import com.minewright.llm.TaskPlanner;
import com.minewright.llm.async.LLMResponse;
import com.minewright.llm.cascade.CascadeRouter;
import com.minewright.llm.cascade.TaskComplexity;
import com.minewright.coordination.ContractNetManager;
import com.minewright.coordination.TaskBid;
import com.minewright.skill.ExecutableSkill;
import com.minewright.skill.Skill;
import com.minewright.skill.SkillLibrary;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Central orchestrator that coordinates all Steve AI subsystems.
 *
 * <p><b>Flow:</b> Command → Skills → Router → Planner → Prioritizer → Executor</p>
 *
 * <p><b>Architecture:</b></p>
 * <pre>
 * User Command
 *     │
 *     ├─► SkillLibrary (check for applicable skill)
 *     │   ├─► Skill found? → Execute skill template
 *     │   └─► No skill? → Continue to LLM
 *     │
 *     ├─► CascadeRouter (complexity analysis & tier selection)
 *     │   ├─► Trivial tasks → Cache or skip LLM
 *     │   ├─► Simple tasks → Fast tier (Groq)
 *     │   ├─► Medium tasks → Standard tier (OpenAI)
 *     │   └─► Complex tasks → Premium tier (GPT-4)
 *     │
 *     ├─► TaskPlanner (LLM-based planning)
 *     │   └─► Generate structured task list
 *     │
 *     ├─► TaskPrioritizer (utility-based scoring)
 *     │   └─► Sort tasks by priority
 *     │
 *     ├─► ContractNetManager (multi-agent coordination)
 *     │   ├─► Single agent? → Execute locally
 *     │   └─► Multi-agent? → Announce tasks, collect bids, award contracts
 *     │
 *     └─► Execution (via ActionExecutor)
 *         └─► Report completion → Update skill library
 * </pre>
 *
 * <p><b>Key Features:</b></p>
 * <ul>
 *   <li><b>Skill Learning:</b> Successful patterns are learned and reused</li>
 *   <li><b>Cost Optimization:</b> Cascade routing minimizes LLM costs</li>
 *   <li><b>Multi-Agent:</b> Contract Net Protocol for task distribution</li>
 *   <li><b>Shared Knowledge:</b> Blackboard pattern for information sharing</li>
 *   <li><b>Resilient:</b> Graceful degradation when subsystems fail</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b></p>
 * <ul>
 *   <li>All operations are thread-safe</li>
 *   <li>Uses concurrent collections for shared state</li>
 *   <li>Async operations use CompletableFuture</li>
 * </ul>
 *
 * @since 1.6.0
 */
public class SteveOrchestrator {

    private static final Logger LOGGER = TestLogger.getLogger(SteveOrchestrator.class);

    // ------------------------------------------------------------------------
    // Dependencies
    // ------------------------------------------------------------------------

    private final SkillLibrary skillLibrary;
    private final CascadeRouter cascadeRouter;
    private final TaskPlanner taskPlanner;
    private final TaskPrioritizer prioritizer;
    private final ContractNetManager contractNet;
    private final Blackboard blackboard;
    private final CommunicationBus commBus;

    // ------------------------------------------------------------------------
    // State
    // ------------------------------------------------------------------------

    private final Map<UUID, List<Task>> pendingTasks;
    private final Map<String, ProcessingStats> processingStats;

    // ------------------------------------------------------------------------
    // Configuration
    // ------------------------------------------------------------------------

    private boolean skillLearningEnabled = true;
    private boolean multiAgentEnabled = true;
    private boolean blackboardEnabled = true;

    /**
     * Creates a new SteveOrchestrator with all subsystems.
     *
     * @param skillLibrary   Skill library for pattern learning
     * @param cascadeRouter  Cascade router for LLM tier selection
     * @param taskPlanner    Task planner for LLM-based planning
     * @param prioritizer    Task prioritizer for utility-based scoring
     * @param contractNet    Contract Net manager for multi-agent coordination
     * @param blackboard     Blackboard for shared knowledge
     * @param commBus        Communication bus for inter-agent messaging
     */
    public SteveOrchestrator(
        SkillLibrary skillLibrary,
        CascadeRouter cascadeRouter,
        TaskPlanner taskPlanner,
        TaskPrioritizer prioritizer,
        ContractNetManager contractNet,
        Blackboard blackboard,
        CommunicationBus commBus
    ) {
        this.skillLibrary = skillLibrary;
        this.cascadeRouter = cascadeRouter;
        this.taskPlanner = taskPlanner;
        this.prioritizer = prioritizer;
        this.contractNet = contractNet;
        this.blackboard = blackboard;
        this.commBus = commBus;

        this.pendingTasks = new ConcurrentHashMap<>();
        this.processingStats = new ConcurrentHashMap<>();

        LOGGER.info("SteveOrchestrator initialized with all subsystems");
    }

    // ------------------------------------------------------------------------
    // Main Processing Flow
    // ------------------------------------------------------------------------

    /**
     * Processes a natural language command through the full integration pipeline.
     *
     * <p><b>Processing Steps:</b></p>
     * <ol>
     *   <li>Check skill library for applicable patterns</li>
     *   <li>If no skill, analyze complexity via cascade router</li>
     *   <li>Get LLM plan via appropriate tier</li>
     *   <li>Prioritize tasks using utility scoring</li>
     *   <li>Coordinate multi-agent execution if needed</li>
     *   <li>Return prioritized task list</li>
     * </ol>
     *
     * @param foreman The foreman entity processing the command
     * @param command The natural language command
     * @return CompletableFuture with prioritized task list
     */
    public CompletableFuture<List<Task>> processCommand(ForemanEntity foreman, String command) {
        UUID agentId = foreman.getUUID();
        long startTime = System.currentTimeMillis();

        LOGGER.info("[Orchestrator] Processing command for '{}': {}",
            foreman.getEntityName(), command);

        // Step 1: Check skill library for applicable skill
        List<Task> tasksFromSkill = findApplicableSkill(command, foreman);
        if (tasksFromSkill != null && !tasksFromSkill.isEmpty()) {
            LOGGER.info("[Orchestrator] Using skill library: {} tasks",
                tasksFromSkill.size());

            // Still prioritize for consistency
            DecisionContext context = DecisionContext.of(foreman, tasksFromSkill);
            List<Task> prioritized = prioritizer.prioritize(tasksFromSkill, context);

            recordProcessing(command, "skill_library", System.currentTimeMillis() - startTime);
            return CompletableFuture.completedFuture(prioritized);
        }

        // Step 2: No skill found, route through cascade
        LOGGER.debug("[Orchestrator] No applicable skill, routing via cascade");

        return processViaCascade(foreman, command, startTime);
    }

    /**
     * Processes a command via the cascade router when no skill is available.
     *
     * @param foreman    The foreman entity
     * @param command    The command
     * @param startTime  Start time for latency tracking
     * @return CompletableFuture with prioritized tasks
     */
    private CompletableFuture<List<Task>> processViaCascade(
        ForemanEntity foreman,
        String command,
        long startTime
    ) {
        // Build context for routing
        Map<String, Object> context = buildRoutingContext(foreman, command);

        // Route through cascade (this handles complexity analysis and tier selection)
        return cascadeRouter.route(command, context)
            .thenCompose(response -> {
                // Step 3: Parse LLM response into tasks
                List<Task> tasks = parseTasksFromResponse(response, command);
                if (tasks.isEmpty()) {
                    LOGGER.warn("[Orchestrator] No tasks generated for command: {}",
                        command);
                    recordProcessing(command, "no_tasks", System.currentTimeMillis() - startTime);
                    return CompletableFuture.completedFuture(new ArrayList<Task>());
                }

                LOGGER.info("[Orchestrator] Generated {} tasks from LLM", tasks.size());

                // Step 4: Prioritize tasks using utility scoring
                DecisionContext decisionContext = DecisionContext.of(foreman, tasks);
                List<Task> prioritized = prioritizer.prioritize(tasks, decisionContext);

                LOGGER.info("[Orchestrator] Prioritized {} tasks", prioritized.size());

                // Step 5: Post to blackboard for visibility
                if (blackboardEnabled) {
                    postToBlackboard(foreman, command, prioritized);
                }

                recordProcessing(command, "cascade_llm", System.currentTimeMillis() - startTime);

                return CompletableFuture.completedFuture(prioritized);
            })
            .exceptionally(throwable -> {
                LOGGER.error("[Orchestrator] Error processing command: {}",
                    throwable.getMessage(), throwable);
                recordProcessing(command, "error", System.currentTimeMillis() - startTime);
                return new ArrayList<Task>();
            });
    }

    // ------------------------------------------------------------------------
    // Skill Library Integration
    // ------------------------------------------------------------------------

    /**
     * Searches the skill library for applicable skills to the command.
     *
     * @param command The command
     * @param foreman The foreman entity
     * @return List of tasks from skill, or null if no applicable skill
     */
    private List<Task> findApplicableSkill(String command, ForemanEntity foreman) {
        if (!skillLearningEnabled) {
            return null;
        }

        // Search for applicable skills
        List<Skill> applicableSkills = skillLibrary.semanticSearch(command);
        if (applicableSkills.isEmpty()) {
            return null;
        }

        // Get the best matching skill
        Skill bestSkill = applicableSkills.get(0);

        LOGGER.info("[Orchestrator] Found applicable skill: {} (confidence: {:.2f})",
            bestSkill.getName(), bestSkill.getSuccessRate());

        // If this is an ExecutableSkill, instantiate it
        if (bestSkill instanceof ExecutableSkill executableSkill) {
            return instantiateSkill(executableSkill, command, foreman);
        }

        // For non-executable skills, we'd need to handle differently
        // For now, return null to fall through to LLM
        return null;
    }

    /**
     * Instantiates an executable skill with the given parameters.
     *
     * @param skill   The executable skill
     * @param command The command
     * @param foreman The foreman entity
     * @return List of tasks from the skill
     */
    private List<Task> instantiateSkill(ExecutableSkill skill, String command, ForemanEntity foreman) {
        // Extract parameters from command
        Map<String, Object> params = extractSkillParameters(skill, command, foreman);

        // Create task from skill template
        List<String> requiredActions = skill.getRequiredActions();
        if (requiredActions.isEmpty()) {
            return List.of();
        }
        Task task = new Task(requiredActions.get(0), params);

        LOGGER.debug("[Orchestrator] Instantiated skill '{}' with {} parameters",
            skill.getName(), params.size());

        return List.of(task);
    }

    /**
     * Extracts parameters for a skill from the command.
     *
     * @param skill   The skill
     * @param command The command
     * @param foreman The foreman entity
     * @return Parameter map
     */
    private Map<String, Object> extractSkillParameters(ExecutableSkill skill, String command,
                                                        ForemanEntity foreman) {
        Map<String, Object> params = new HashMap<>();

        // Add foreman position
        params.put("startX", foreman.getBlockX());
        params.put("startY", foreman.getBlockY());
        params.put("startZ", foreman.getBlockZ());

        // Add common defaults
        params.put("depth", 10);
        params.put("length", 20);
        params.put("direction", "north");
        params.put("block", "oak_planks");
        params.put("size", 5);

        // TODO: Extract actual parameters from command using NLP

        return params;
    }

    // ------------------------------------------------------------------------
    // Multi-Agent Coordination
    // ------------------------------------------------------------------------

    /**
     * Coordinates multi-agent execution using Contract Net Protocol.
     *
     * @param foreman     The requesting foreman
     * @param tasks       Tasks to distribute
     * @return CompletableFuture that completes when all tasks are awarded
     */
    public CompletableFuture<Void> coordinateMultiAgent(ForemanEntity foreman, List<Task> tasks) {
        if (!multiAgentEnabled || tasks.size() <= 1) {
            // Single agent or disabled, return immediately
            return CompletableFuture.completedFuture(null);
        }

        LOGGER.info("[Orchestrator] Coordinating {} tasks across agents", tasks.size());

        // Announce each task for bidding
        List<CompletableFuture<Void>> awardFutures = new ArrayList<>();

        for (Task task : tasks) {
            awardFutures.add(announceTaskForBidding(foreman, task));
        }

        // Wait for all tasks to be awarded
        return CompletableFuture.allOf(awardFutures.toArray(new CompletableFuture<?>[0]));
    }

    /**
     * Announces a task for bidding via Contract Net Protocol.
     *
     * @param foreman The foreman announcing the task
     * @param task    The task to announce
     * @return CompletableFuture that completes when contract is awarded
     */
    private CompletableFuture<Void> announceTaskForBidding(ForemanEntity foreman, Task task) {
        String announcementId = contractNet.announceTask(task, foreman.getUUID());

        // Wait for bids and award to best bidder
        return CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(5000); // Wait for bids (5 second deadline)

                Optional<TaskBid> winner = contractNet.selectWinner(announcementId);
                if (winner.isPresent()) {
                    contractNet.awardContract(announcementId, winner.get());
                    LOGGER.info("[Orchestrator] Awarded task '{}' to agent {}",
                        task.getAction(), winner.get().bidderId());
                } else {
                    LOGGER.warn("[Orchestrator] No bids received for task '{}'",
                        task.getAction());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.warn("[Orchestrator] Bidding interrupted for task '{}'",
                    task.getAction());
            }
        });
    }

    // ------------------------------------------------------------------------
    // Task Completion Callbacks
    // ------------------------------------------------------------------------

    /**
     * Called when a task is completed.
     *
     * <p>Updates skill library with outcome and posts to blackboard.</p>
     *
     * @param task    The completed task
     * @param success Whether the task succeeded
     */
    public void onTaskComplete(Task task, boolean success) {
        // Update skill library if this was from a skill
        if (skillLearningEnabled) {
            List<Skill> applicableSkills = skillLibrary.findApplicableSkills(task);
            for (Skill skill : applicableSkills) {
                skillLibrary.recordOutcome(skill.getName(), success);
            }
        }

        // Post to blackboard
        if (blackboardEnabled) {
            BlackboardEntry<Boolean> entry = BlackboardEntry.createFact(
                "task_complete_" + task.hashCode(),
                success,
                null
            );
            blackboard.post(KnowledgeArea.TASKS, entry);
        }

        LOGGER.debug("[Orchestrator] Task completion recorded: {} (success: {})",
            task.getAction(), success);
    }

    /**
     * Called when a task fails.
     *
     * @param task    The failed task
     * @param reason  Failure reason
     */
    public void onTaskFailed(Task task, String reason) {
        onTaskComplete(task, false);

        LOGGER.warn("[Orchestrator] Task failed: {} - {}", task.getAction(), reason);
    }

    // ------------------------------------------------------------------------
    // Blackboard Integration
    // ------------------------------------------------------------------------

    /**
     * Posts task information to the blackboard.
     *
     * @param foreman   The foreman entity
     * @param command   The original command
     * @param tasks     The prioritized tasks
     */
    private void postToBlackboard(ForemanEntity foreman, String command, List<Task> tasks) {
        BlackboardEntry<String> commandEntry = BlackboardEntry.createFact(
            "command_" + foreman.getUUID(),
            command,
            foreman.getUUID()
        );
        blackboard.post(KnowledgeArea.TASKS, commandEntry);

        BlackboardEntry<List<Task>> tasksEntry = BlackboardEntry.createFact(
            "tasks_" + foreman.getUUID(),
            tasks,
            foreman.getUUID()
        );
        blackboard.post(KnowledgeArea.TASKS, tasksEntry);
    }

    // ------------------------------------------------------------------------
    // Communication Integration
    // ------------------------------------------------------------------------

    /**
     * Sends a message to another agent.
     *
     * @param message The message to send
     */
    public void sendMessage(AgentMessage message) {
        commBus.send(message);
    }

    /**
     * Broadcasts a message to all agents.
     *
     * @param senderId The sender
     * @param type     Message type
     * @param content  Message content
     */
    public void broadcast(UUID senderId, AgentMessage.MessageType type, String content) {
        commBus.broadcast(senderId, type, content);
    }

    // ------------------------------------------------------------------------
    // Utility Methods
    // ------------------------------------------------------------------------

    /**
     * Builds routing context for cascade router.
     *
     * @param foreman The foreman entity
     * @param command The command
     * @return Context map
     */
    private Map<String, Object> buildRoutingContext(ForemanEntity foreman, String command) {
        Map<String, Object> context = new HashMap<>();
        context.put("foreman", foreman);
        context.put("command", command);
        context.put("entityName", foreman.getEntityName());
        context.put("providerId", "orchestrator");

        return context;
    }

    /**
     * Parses tasks from LLM response.
     *
     * @param response The LLM response
     * @param command  The original command
     * @return List of parsed tasks
     */
    private List<Task> parseTasksFromResponse(LLMResponse response, String command) {
        String content = response.getContent();
        ResponseParser.ParsedResponse parsed = ResponseParser.parseAIResponse(content);

        if (parsed == null) {
            return List.of();
        }

        return parsed.getTasks();
    }

    /**
     * Records processing statistics.
     *
     * @param command    The command
     * @param method     Processing method used
     * @param latencyMs  Processing latency
     */
    private void recordProcessing(String command, String method, long latencyMs) {
        ProcessingStats stats = processingStats.computeIfAbsent(method, k -> new ProcessingStats());
        stats.recordCommand(latencyMs);

        LOGGER.debug("[Orchestrator] Processed via {} in {}ms: {}",
            method, latencyMs, truncate(command));
    }

    /**
     * Truncates string for logging.
     */
    private static String truncate(String str) {
        if (str == null) return "null";
        return str.length() > 50 ? str.substring(0, 47) + "..." : str;
    }

    // ------------------------------------------------------------------------
    // Getters
    // ------------------------------------------------------------------------

    public SkillLibrary getSkillLibrary() {
        return skillLibrary;
    }

    public CascadeRouter getCascadeRouter() {
        return cascadeRouter;
    }

    public TaskPlanner getTaskPlanner() {
        return taskPlanner;
    }

    public TaskPrioritizer getPrioritizer() {
        return prioritizer;
    }

    public ContractNetManager getContractNet() {
        return contractNet;
    }

    public Blackboard getBlackboard() {
        return blackboard;
    }

    public CommunicationBus getCommunicationBus() {
        return commBus;
    }

    public boolean isSkillLearningEnabled() {
        return skillLearningEnabled;
    }

    public void setSkillLearningEnabled(boolean enabled) {
        this.skillLearningEnabled = enabled;
    }

    public boolean isMultiAgentEnabled() {
        return multiAgentEnabled;
    }

    public void setMultiAgentEnabled(boolean enabled) {
        this.multiAgentEnabled = enabled;
    }

    public boolean isBlackboardEnabled() {
        return blackboardEnabled;
    }

    public void setBlackboardEnabled(boolean enabled) {
        this.blackboardEnabled = enabled;
    }

    // ------------------------------------------------------------------------
    // Inner Classes
    // ------------------------------------------------------------------------

    /**
     * Processing statistics tracking.
     */
    private static class ProcessingStats {
        private long totalCommands = 0;
        private long totalLatencyMs = 0;
        private long maxLatencyMs = 0;

        public void recordCommand(long latencyMs) {
            totalCommands++;
            totalLatencyMs += latencyMs;
            maxLatencyMs = Math.max(maxLatencyMs, latencyMs);
        }

        public double getAverageLatencyMs() {
            return totalCommands > 0 ? (double) totalLatencyMs / totalCommands : 0;
        }

        public long getMaxLatencyMs() {
            return maxLatencyMs;
        }
    }
}
