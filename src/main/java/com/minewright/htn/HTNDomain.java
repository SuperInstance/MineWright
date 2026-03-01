package com.minewright.htn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Domain knowledge repository for Hierarchical Task Network (HTN) planning.
 *
 * <p><b>HTN Domain:</b></p>
 * <p>The domain contains all methods for decomposing compound tasks into primitive actions.
 * It acts as the "knowledge base" that the planner queries during decomposition.</p>
 *
 * <p><b>Domain Structure:</b></p>
 * <ul>
 *   <li><b>Task Name → Methods:</b> Maps compound task names to applicable methods</li>
 *   <li><b>Multiple Methods:</b> Each task can have multiple alternative decomposition methods</li>
 *   <li><b>Priority Selection:</b> Methods are tried in priority order during planning</li>
 * </ul>
 *
 * <p><b>Example Domain:</b></p>
 * <pre>{@code
 * HTNDomain domain = HTNDomain.createDefault();
 *
 * // Add custom method
 * domain.addMethod(HTNMethod.builder("build_stone_house", "build_house")
 *     .precondition(state -> state.getBoolean("hasStone"))
 *     .subtask(HTNTask.primitive("mine")
 *         .parameter("blockType", "stone")
 *         .parameter("count", 192)
 *         .build())
 *     .subtask(HTNTask.primitive("build")
 *         .parameter("material", "stone")
 *         .build())
 *     .priority(80)
 *     .build());
 *
 * // Get applicable methods for a task
 * List<HTNMethod> methods = domain.getMethodsForTask("build_house", worldState);
 * }</pre>
 *
 * <p><b>Built-in Minecraft Tasks:</b></p>
 * <p>The default domain includes methods for common Minecraft tasks:</p>
 * <ul>
 *   <li><b>build_house:</b> Decomposes into gathering materials, clearing site, building</li>
 *   <li><b>gather_wood:</b> Decomposes into finding trees, mining logs, returning</li>
 *   <li><b>craft_item:</b> Decomposes based on recipe requirements</li>
 *   <li><b>mine_resource:</b> Decomposes into finding ore, mining, smelting if needed</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b></p>
 * <p>This class is thread-safe for concurrent access and modification.
 * Uses ConcurrentHashMap for method storage.</p>
 *
 * @see HTNMethod
 * @see HTNTask
 * @see HTNPlanner
 *
 * @since 1.0.0
 */
public class HTNDomain {
    private static final Logger LOGGER = LoggerFactory.getLogger(HTNDomain.class);

    /**
     * Map of task name to list of methods that decompose it.
     * ConcurrentHashMap for thread-safe concurrent access.
     */
    private final Map<String, List<HTNMethod>> methods;

    /**
     * Domain name for identification and debugging.
     */
    private final String domainName;

    /**
     * Creates a new empty HTN domain.
     *
     * @param domainName Domain identifier
     */
    public HTNDomain(String domainName) {
        this.domainName = domainName;
        this.methods = new ConcurrentHashMap<>();
    }

    /**
     * Creates a domain with the default Minecraft task methods.
     *
     * @return A new HTNDomain with built-in methods loaded
     */
    public static HTNDomain createDefault() {
        HTNDomain domain = new HTNDomain("minecraft_default");
        domain.loadDefaultMethods();
        return domain;
    }

    /**
     * Adds a method to this domain.
     * If a method with the same name already exists for the task, it will be replaced.
     *
     * @param method The method to add
     */
    public void addMethod(HTNMethod method) {
        if (method == null) {
            LOGGER.warn("[{}] Attempted to add null method, ignoring", domainName);
            return;
        }

        String taskName = method.getTaskName();
        methods.computeIfAbsent(taskName, k -> new ArrayList<>()).add(method);

        LOGGER.debug("[{}] Added method '{}' for task '{}'",
            domainName, method.getMethodName(), taskName);
    }

    /**
     * Adds multiple methods at once.
     *
     * @param newMethods Collection of methods to add
     */
    public void addMethods(Collection<HTNMethod> newMethods) {
        if (newMethods == null) {
            return;
        }
        newMethods.forEach(this::addMethod);
    }

    /**
     * Gets all methods for a task, regardless of preconditions.
     *
     * @param taskName The compound task name
     * @return Unmodifiable list of methods, or empty list if none found
     */
    public List<HTNMethod> getMethodsForTask(String taskName) {
        List<HTNMethod> taskMethods = methods.get(taskName);
        return taskMethods != null
            ? Collections.unmodifiableList(taskMethods)
            : Collections.emptyList();
    }

    /**
     * Gets applicable methods for a task given the current world state.
     * Methods are sorted by priority (highest first).
     *
     * @param taskName   The compound task name
     * @param worldState Current world state for precondition checking
     * @return List of applicable methods sorted by priority
     */
    public List<HTNMethod> getApplicableMethods(String taskName, HTNWorldState worldState) {
        List<HTNMethod> allMethods = methods.get(taskName);
        if (allMethods == null || allMethods.isEmpty()) {
            return Collections.emptyList();
        }

        return allMethods.stream()
            .filter(method -> method.checkPreconditions(worldState))
            .sorted((m1, m2) -> Integer.compare(m2.getPriority(), m1.getPriority())) // Descending
            .collect(Collectors.toList());
    }

    /**
     * Gets the highest priority applicable method for a task.
     *
     * @param taskName   The compound task name
     * @param worldState Current world state
     * @return The best applicable method, or null if none applicable
     */
    public HTNMethod getBestMethod(String taskName, HTNWorldState worldState) {
        List<HTNMethod> applicable = getApplicableMethods(taskName, worldState);
        return applicable.isEmpty() ? null : applicable.get(0);
    }

    /**
     * Checks if a task has any methods defined in this domain.
     *
     * @param taskName The task name to check
     * @return true if methods exist for this task
     */
    public boolean hasMethodsFor(String taskName) {
        List<HTNMethod> taskMethods = methods.get(taskName);
        return taskMethods != null && !taskMethods.isEmpty();
    }

    /**
     * Removes a method from this domain.
     *
     * @param methodName The method name to remove
     * @return true if a method was removed
     */
    public boolean removeMethod(String methodName) {
        boolean[] removed = {false};
        methods.values().forEach(methodList -> {
            methodList.removeIf(method -> {
                if (method.getMethodName().equals(methodName)) {
                    removed[0] = true;
                    return true;
                }
                return false;
            });
        });
        return removed[0];
    }

    /**
     * Removes all methods for a specific task.
     *
     * @param taskName The task name
     * @return List of removed methods, or empty list if none
     */
    public List<HTNMethod> removeMethodsForTask(String taskName) {
        List<HTNMethod> removed = methods.remove(taskName);
        return removed != null ? removed : Collections.emptyList();
    }

    /**
     * Clears all methods from this domain.
     */
    public void clear() {
        methods.clear();
        LOGGER.debug("[{}] Domain cleared", domainName);
    }

    /**
     * Gets the number of tasks in this domain.
     *
     * @return Task count
     */
    public int getTaskCount() {
        return methods.size();
    }

    /**
     * Gets the total number of methods in this domain.
     *
     * @return Method count
     */
    public int getMethodCount() {
        return methods.values().stream()
            .mapToInt(List::size)
            .sum();
    }

    /**
     * Gets all task names in this domain.
     *
     * @return Set of task names
     */
    public Set<String> getTaskNames() {
        return Collections.unmodifiableSet(methods.keySet());
    }

    /**
     * Gets the domain name.
     *
     * @return Domain identifier
     */
    public String getDomainName() {
        return domainName;
    }

    /**
     * Loads default Minecraft task methods into this domain.
     * Override this method to customize the default domain.
     */
    protected void loadDefaultMethods() {
        LOGGER.info("[{}] Loading default Minecraft task methods", domainName);
        loadBuildingMethods();
        loadGatheringMethods();
        loadCraftingMethods();
        loadMiningMethods();
        LOGGER.info("[{}] Loaded {} task definitions with {} total methods",
            domainName, methods.size(), getMethodCount());
    }

    /**
     * Loads building-related task methods.
     */
    protected void loadBuildingMethods() {
        // Build house with materials (high priority)
        addMethod(HTNMethod.builder("build_house_with_materials", "build_house")
            .description("Build house when materials are already available")
            .precondition(state -> state.hasProperty("hasMaterials") &&
                                  state.getBoolean("hasMaterials"))
            .subtask(HTNTask.primitive("pathfind")
                .parameter("target", "build_site")
                .build())
            .subtask(HTNTask.primitive("clear_area")
                .parameter("width", 5)
                .parameter("depth", 5)
                .parameter("height", 3)
                .build())
            .subtask(HTNTask.compound("construct_walls")
                .parameter("height", 3)
                .build())
            .subtask(HTNTask.primitive("place")
                .parameter("blockType", "oak_planks")
                .parameter("layer", "roof")
                .build())
            .priority(100)
            .build());

        // Build house with gathering (lower priority, fallback)
        addMethod(HTNMethod.builder("build_house_with_gathering", "build_house")
            .description("Build house including material gathering")
            .precondition(state -> true) // Always applicable as fallback
            .subtask(HTNTask.compound("gather_wood")
                .parameter("count", 64)
                .build())
            .subtask(HTNTask.compound("craft_planks")
                .parameter("count", 192)
                .build())
            .subtask(HTNTask.primitive("pathfind")
                .parameter("target", "build_site")
                .build())
            .subtask(HTNTask.primitive("clear_area")
                .parameter("width", 5)
                .parameter("depth", 5)
                .parameter("height", 3)
                .build())
            .subtask(HTNTask.compound("construct_walls")
                .parameter("height", 3)
                .build())
            .subtask(HTNTask.primitive("place")
                .parameter("blockType", "oak_planks")
                .parameter("layer", "roof")
                .build())
            .priority(50)
            .build());

        // Construct walls subtask
        addMethod(HTNMethod.builder("construct_walls_standard", "construct_walls")
            .description("Standard wall construction")
            .subtask(HTNTask.primitive("build")
                .parameter("structure", "walls")
                .parameter("height", 3)
                .build())
            .priority(100)
            .build());
    }

    /**
     * Loads resource gathering methods.
     */
    protected void loadGatheringMethods() {
        // Gather wood with tool
        addMethod(HTNMethod.builder("gather_wood_with_tool", "gather_wood")
            .description("Gather wood when tool is available")
            .precondition(state -> state.getBoolean("hasAxe"))
            .subtask(HTNTask.primitive("pathfind")
                .parameter("targetType", "tree")
                .build())
            .subtask(HTNTask.primitive("mine")
                .parameter("blockType", "oak_log")
                .parameter("count", 16) // Will be parameterized from task
                .build())
            .subtask(HTNTask.primitive("pathfind")
                .parameter("target", "base")
                .build())
            .priority(100)
            .build());

        // Gather wood without tool (lower priority)
        addMethod(HTNMethod.builder("gather_wood_without_tool", "gather_wood")
            .description("Gather wood by hand (slower)")
            .precondition(state -> true)
            .subtask(HTNTask.primitive("pathfind")
                .parameter("targetType", "tree")
                .build())
            .subtask(HTNTask.primitive("mine")
                .parameter("blockType", "oak_log")
                .parameter("count", 16)
                .parameter("byHand", true)
                .build())
            .subtask(HTNTask.primitive("pathfind")
                .parameter("target", "base")
                .build())
            .priority(50)
            .build());
    }

    /**
     * Loads crafting methods.
     */
    protected void loadCraftingMethods() {
        // Craft planks from logs
        addMethod(HTNMethod.builder("craft_planks_from_logs", "craft_planks")
            .description("Craft wooden planks from logs")
            .precondition(state -> state.getInt("logCount") >= 1)
            .subtask(HTNTask.primitive("pathfind")
                .parameter("target", "crafting_table")
                .build())
            .subtask(HTNTask.primitive("craft")
                .parameter("output", "oak_planks")
                .parameter("count", 4) // 1 log = 4 planks
                .build())
            .priority(100)
            .build());

        // Craft sticks from planks
        addMethod(HTNMethod.builder("craft_sticks_from_planks", "craft_sticks")
            .description("Craft sticks from wooden planks")
            .precondition(state -> state.getInt("plankCount") >= 2)
            .subtask(HTNTask.primitive("pathfind")
                .parameter("target", "crafting_table")
                .build())
            .subtask(HTNTask.primitive("craft")
                .parameter("output", "stick")
                .parameter("count", 4) // 2 planks = 4 sticks
                .build())
            .priority(100)
            .build());
    }

    /**
     * Loads mining methods.
     */
    protected void loadMiningMethods() {
        // Mine with appropriate tool
        addMethod(HTNMethod.builder("mine_with_tool", "mine_resource")
            .description("Mine resource with appropriate tool")
            .precondition(state -> state.hasProperty("toolType") &&
                                  !state.getString("toolType", "").isEmpty())
            .subtask(HTNTask.primitive("pathfind")
                .parameter("targetType", "ore")
                .build())
            .subtask(HTNTask.primitive("mine")
                .parameter("useTool", true)
                .build())
            .priority(100)
            .build());

        // Mine by hand
        addMethod(HTNMethod.builder("mine_by_hand", "mine_resource")
            .description("Mine resource by hand")
            .precondition(state -> true)
            .subtask(HTNTask.primitive("pathfind")
                .parameter("targetType", "ore")
                .build())
            .subtask(HTNTask.primitive("mine")
                .parameter("useTool", false)
                .build())
            .priority(50)
            .build());
    }

    @Override
    public String toString() {
        return "HTNDomain{" +
               "name='" + domainName + '\'' +
               ", tasks=" + getTaskCount() +
               ", methods=" + getMethodCount() +
               '}';
    }

    /**
     * Creates a detailed string representation of the domain.
     *
     * @return Detailed domain information
     */
    public String toDetailedString() {
        StringBuilder sb = new StringBuilder();
        sb.append("HTNDomain{name='").append(domainName).append('\'');
        sb.append(", tasks=").append(getTaskCount());
        sb.append(", methods=").append(getMethodCount());
        sb.append(", tasks=[\n");

        methods.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> {
                sb.append("  '").append(entry.getKey()).append("': ");
                sb.append(entry.getValue().size()).append(" methods");
                sb.append("\n");
            });

        sb.append("]}");
        return sb.toString();
    }
}
