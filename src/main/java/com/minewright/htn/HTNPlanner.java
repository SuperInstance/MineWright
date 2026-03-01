package com.minewright.htn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Hierarchical Task Network (HTN) planner for decomposing compound tasks into executable actions.
 *
 * <p><b>HTN Planning:</b></p>
 * <p>HTN planning works by recursively decomposing high-level compound tasks into
 * primitive executable tasks. Unlike GOAP (which works backward from goals), HTN
 * uses forward decomposition through methods that define alternative ways to
 * achieve compound tasks.</p>
 *
 * <p><b>Planning Algorithm:</b></p>
 * <pre>
 * decompose(task, state, depth):
 *   if task.isPrimitive():
 *     return [task]  // Base case: primitive task is executable
 *   if task.isCompound():
 *     methods = domain.getApplicableMethods(task.name, state)
 *     for method in methods (by priority):
 *       try:
 *         subtasks = []
 *         for subtask in method.subtasks:
 *           decomposed = decompose(subtask, state, depth + 1)
 *           if decomposed == null:  // Decomposition failed
 *             break  // Try next method
 *           subtasks.addAll(decomposed)
 *         return subtasks  // Success: all subtasks decomposed
 *       catch InfiniteLoopException:
 *         continue  // Try next method
 *     return null  // No method succeeded
 * </pre>
 *
 * <p><b>Key Features:</b></p>
 * <ul>
 *   <li><b>Forward Decomposition:</b> Decomposes compound tasks into primitive actions</li>
 *   <li><b>Method Selection:</b> Tries methods in priority order, backtracks on failure</li>
 *   <li><b>Loop Detection:</b> Detects infinite loops during decomposition</li>
 *   <li><b>Depth Limiting:</b> Prevents runaway recursion</li>
 *   <li><b>Immutable State:</b> Uses snapshots for safe backtracking</li>
 * </ul>
 *
 * <p><b>Example Usage:</b></p>
 * <pre>{@code
 * // Create planner with domain
 * HTNPlanner planner = new HTNPlanner(HTNDomain.createDefault());
 *
 * // Create initial world state
 * HTNWorldState state = HTNWorldState.builder()
 *     .property("hasWood", false)
 *     .property("hasAxe", true)
 *     .build();
 *
 * // Decompose a compound task
 * HTNTask buildHouse = HTNTask.compound("build_house")
 *     .parameter("material", "oak_planks")
 *     .build();
 *
 * List<HTNTask> primitiveTasks = planner.decompose(buildHouse, state);
 *
 * // Convert to executable actions
 * List<com.minewright.action.Task> actions = primitiveTasks.stream()
 *     .map(HTNTask::toActionTask)
 *     .collect(Collectors.toList());
 * }</pre>
 *
 * <p><b>Thread Safety:</b></p>
 * <p>This class is thread-safe for concurrent planning operations.
 * Each planning call uses its own state and tracking structures.</p>
 *
 * @see HTNTask
 * @see HTNMethod
 * @see HTNDomain
 * @see HTNWorldState
 *
 * @since 1.0.0
 */
public class HTNPlanner {
    private static final Logger LOGGER = LoggerFactory.getLogger(HTNPlanner.class);

    /**
     * Default maximum decomposition depth to prevent infinite loops.
     * Can be overridden per planning call.
     */
    private static final int DEFAULT_MAX_DEPTH = 50;

    /**
     * Default maximum decomposition iterations.
     */
    private static final int DEFAULT_MAX_ITERATIONS = 1000;

    /**
     * The domain containing all task methods.
     */
    private final HTNDomain domain;

    /**
     * Maximum decomposition depth.
     */
    private final int maxDepth;

    /**
     * Maximum decomposition iterations.
     */
    private final int maxIterations;

    /**
     * Creates a new HTN planner with the specified domain.
     *
     * @param domain The HTN domain containing task methods
     */
    public HTNPlanner(HTNDomain domain) {
        this(domain, DEFAULT_MAX_DEPTH, DEFAULT_MAX_ITERATIONS);
    }

    /**
     * Creates a new HTN planner with custom limits.
     *
     * @param domain       The HTN domain
     * @param maxDepth     Maximum decomposition depth
     * @param maxIterations Maximum decomposition iterations
     */
    public HTNPlanner(HTNDomain domain, int maxDepth, int maxIterations) {
        if (domain == null) {
            throw new IllegalArgumentException("Domain cannot be null");
        }
        this.domain = domain;
        this.maxDepth = maxDepth;
        this.maxIterations = maxIterations;
        LOGGER.debug("HTNPlanner initialized with domain '{}' (maxDepth={}, maxIterations={})",
            domain.getDomainName(), maxDepth, maxIterations);
    }

    /**
     * Gets the domain used by this planner.
     *
     * @return The HTN domain
     */
    public HTNDomain getDomain() {
        return domain;
    }

    /**
     * Decomposes a compound task into primitive executable tasks.
     *
     * <p>This is the main planning entry point. It takes a compound task and
     * recursively decomposes it using methods from the domain until only
     * primitive tasks remain.</p>
     *
     * @param rootTask The root compound task to decompose
     * @param worldState Initial world state for precondition evaluation
     * @return List of primitive tasks in execution order, or null if decomposition failed
     */
    public List<HTNTask> decompose(HTNTask rootTask, HTNWorldState worldState) {
        return decompose(rootTask, worldState, maxDepth);
    }

    /**
     * Decomposes a compound task with a custom depth limit.
     *
     * @param rootTask  The root compound task
     * @param worldState Initial world state
     * @param depthLimit Maximum decomposition depth for this call
     * @return List of primitive tasks, or null if failed
     */
    public List<HTNTask> decompose(HTNTask rootTask, HTNWorldState worldState, int depthLimit) {
        if (rootTask == null) {
            LOGGER.warn("Cannot decompose null task");
            return null;
        }

        if (worldState == null) {
            LOGGER.warn("Cannot decompose with null world state");
            return null;
        }

        LOGGER.debug("Starting HTN decomposition: task='{}', depthLimit={}, domain='{}'",
            rootTask.getName(), depthLimit, domain.getDomainName());

        PlanningContext context = new PlanningContext(worldState, depthLimit);
        List<HTNTask> result = decomposeRecursive(rootTask, context, 0);

        if (result != null) {
            LOGGER.info("HTN decomposition SUCCESS: task='{}', primitiveTasks={}, iterations={}, depth={}",
                rootTask.getName(), result.size(), context.iterations.get(), context.maxDepthReached);
        } else {
            LOGGER.warn("HTN decomposition FAILED: task='{}', iterations={}, depth={}",
                rootTask.getName(), context.iterations.get(), context.maxDepthReached);
        }

        return result;
    }

    /**
     * Internal recursive decomposition method.
     *
     * @param task    The task to decompose
     * @param context Planning context tracking state
     * @param depth   Current decomposition depth
     * @return List of decomposed tasks, or null if decomposition failed
     */
    private List<HTNTask> decomposeRecursive(HTNTask task, PlanningContext context, int depth) {
        // Check iteration limit
        if (context.iterations.incrementAndGet() > maxIterations) {
            LOGGER.warn("HTN decomposition exceeded iteration limit: {}", maxIterations);
            return null;
        }

        // Track maximum depth
        if (depth > context.maxDepthReached) {
            context.maxDepthReached = depth;
        }

        // Check depth limit
        if (depth > context.depthLimit) {
            LOGGER.warn("HTN decomposition exceeded depth limit: {}", depth);
            return null;
        }

        // Detect infinite loops
        String taskKey = task.getName() + ":" + depth;
        int visitCount = context.visitedTasks.merge(taskKey, 1, Integer::sum);
        if (visitCount > 3) { // Same task at same depth visited too many times
            LOGGER.warn("Detected potential infinite loop at task '{}' (depth={}, visits={})",
                task.getName(), depth, visitCount);
            return null;
        }

        LOGGER.trace("Decomposing: {} (depth={}, type={})",
            task.getName(), depth, task.getType());

        // Base case: primitive task
        if (task.isPrimitive()) {
            return Collections.singletonList(task);
        }

        // Recursive case: compound task
        if (!task.isCompound()) {
            LOGGER.error("Task has unknown type: {}", task.getType());
            return null;
        }

        // Get applicable methods
        List<HTNMethod> methods = domain.getApplicableMethods(task.getName(), context.worldState);

        if (methods.isEmpty()) {
            LOGGER.debug("No applicable methods for compound task: {}", task.getName());
            return null;
        }

        LOGGER.debug("Found {} applicable methods for task '{}' (depth={})",
            methods.size(), task.getName(), depth);

        // Try each method in priority order
        for (HTNMethod method : methods) {
            LOGGER.debug("Trying method '{}' (priority={}) for task '{}'",
                method.getMethodName(), method.getPriority(), task.getName());

            List<HTNTask> decomposed = tryMethod(method, task, context, depth);
            if (decomposed != null) {
                LOGGER.debug("Method '{}' succeeded for task '{}', produced {} subtasks",
                    method.getMethodName(), task.getName(), decomposed.size());
                return decomposed;
            }

            LOGGER.debug("Method '{}' failed for task '{}', trying next method",
                method.getMethodName(), task.getName());
        }

        // No method succeeded
        LOGGER.debug("All methods failed for compound task: {}", task.getName());
        return null;
    }

    /**
     * Attempts to decompose a task using a specific method.
     *
     * @param method  The method to try
     * @param task    The compound task being decomposed
     * @param context Planning context
     * @param depth   Current depth
     * @return List of decomposed tasks, or null if method failed
     */
    private List<HTNTask> tryMethod(HTNMethod method, HTNTask task,
                                     PlanningContext context, int depth) {
        List<HTNTask> allSubtasks = new ArrayList<>();

        // Decompose each subtask in the method
        for (HTNTask subtask : method.getSubtasks()) {
            // Merge task parameters from parent to subtask
            HTNTask subtaskWithContext = mergeTaskContext(subtask, task);

            List<HTNTask> decomposed = decomposeRecursive(subtaskWithContext, context, depth + 1);
            if (decomposed == null) {
                // Subtask decomposition failed, this method is not viable
                LOGGER.debug("Subtask '{}' decomposition failed for method '{}'",
                    subtask.getName(), method.getMethodName());
                return null;
            }

            allSubtasks.addAll(decomposed);
        }

        // All subtasks decomposed successfully
        return allSubtasks;
    }

    /**
     * Merges context from parent task to subtask.
     * Parameters from parent task are available to subtasks unless overridden.
     *
     * @param subtask The subtask
     * @param parent  The parent compound task
     * @return Subtask with merged parameters
     */
    private HTNTask mergeTaskContext(HTNTask subtask, HTNTask parent) {
        // If subtask has no parameters, it inherits all from parent
        if (subtask.getParameters().isEmpty()) {
            return subtask.withParameters(parent.getParameters());
        }

        // Otherwise, subtask's parameters take precedence
        return subtask;
    }

    /**
     * Planning context for tracking state during decomposition.
     */
    private static class PlanningContext {
        /**
         * Current world state (immutable snapshot).
         */
        final HTNWorldState worldState;

        /**
         * Maximum depth allowed for this planning session.
         */
        final int depthLimit;

        /**
         * Counter for iterations (to detect runaway planning).
         */
        final AtomicInteger iterations = new AtomicInteger(0);

        /**
         * Maximum depth reached during planning.
         */
        int maxDepthReached = 0;

        /**
         * Tracks visited tasks to detect infinite loops.
         * Key: taskName:depth, Value: visit count
         */
        final Map<String, Integer> visitedTasks = new HashMap<>();

        PlanningContext(HTNWorldState worldState, int depthLimit) {
            this.worldState = worldState.snapshot(); // Immutable snapshot
            this.depthLimit = depthLimit;
        }
    }

    /**
     * Checks if a task can be decomposed given the current world state.
     * This is a fast check without performing full decomposition.
     *
     * @param task       The task to check
     * @param worldState Current world state
     * @return true if the task can be decomposed
     */
    public boolean canDecompose(HTNTask task, HTNWorldState worldState) {
        if (task == null || worldState == null) {
            return false;
        }

        if (task.isPrimitive()) {
            return true; // Primitive tasks are already decomposable
        }

        // Check if there are applicable methods
        List<HTNMethod> methods = domain.getApplicableMethods(task.getName(), worldState);
        return !methods.isEmpty();
    }

    /**
     * Gets all applicable methods for a task without decomposing.
     * Useful for debugging and planning visualization.
     *
     * @param task       The compound task
     * @param worldState Current world state
     * @return List of applicable methods
     */
    public List<HTNMethod> getApplicableMethods(HTNTask task, HTNWorldState worldState) {
        if (task == null || worldState == null) {
            return Collections.emptyList();
        }

        if (task.isPrimitive()) {
            return Collections.emptyList();
        }

        return domain.getApplicableMethods(task.getName(), worldState);
    }

    @Override
    public String toString() {
        return "HTNPlanner{" +
               "domain=" + domain.getDomainName() +
               ", maxDepth=" + maxDepth +
               ", maxIterations=" + maxIterations +
               '}';
    }
}
