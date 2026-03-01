/**
 * Hierarchical Task Network (HTN) planning infrastructure for Steve AI.
 *
 * <h2>Overview</h2>
 * <p>HTN planning decomposes high-level compound tasks into primitive executable actions
 * through recursive forward decomposition. Unlike Goal-Oriented Action Planning (GOAP)
 * which works backward from goals, HTN uses designer-specified methods that define
 * alternative ways to achieve compound tasks.</p>
 *
 * <h2>Core Components</h2>
 * <ul>
 *   <li>{@link com.minewright.htn.HTNTask} - Tasks (primitive or compound) with parameters</li>
 *   <li>{@link com.minewright.htn.HTNMethod} - Decomposition methods with preconditions and subtasks</li>
 *   <li>{@link com.minewright.htn.HTNDomain} - Domain knowledge repository containing all methods</li>
 *   <li>{@link com.minewright.htn.HTNPlanner} - The planner that performs decomposition</li>
 *   <li>{@link com.minewright.htn.HTNWorldState} - World state representation for precondition checking</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create planner with default Minecraft domain
 * HTNPlanner planner = new HTNPlanner(HTNDomain.createDefault());
 *
 * // Define world state
 * HTNWorldState state = HTNWorldState.builder()
 *     .property("hasWood", true)
 *     .property("woodCount", 64)
 *     .property("hasAxe", true)
 *     .build();
 *
 * // Create compound task
 * HTNTask buildHouse = HTNTask.compound("build_house")
 *     .parameter("material", "oak_planks")
 *     .parameter("width", 5)
 *     .build();
 *
 * // Decompose into primitive tasks
 * List<HTNTask> primitiveTasks = planner.decompose(buildHouse, state);
 *
 * // Convert to executable actions
 * if (primitiveTasks != null) {
 *     List<Task> actions = primitiveTasks.stream()
 *         .map(HTNTask::toActionTask)
 *         .collect(Collectors.toList());
 *     actionExecutor.queueTasks(actions);
 * }
 * }</pre>
 *
 * <h2>HTN vs GOAP</h2>
 * <table border="1">
 *   <tr><th>Aspect</th><th>HTN</th><th>GOAP</th></tr>
 *   <tr><td>Direction</td><td>Forward (decomposition)</td><td>Backward (from goal)</td></tr>
 *   <tr><td>Knowledge</td><td>Designer-specified methods</td><td>Preconditions/effects</td></tr>
 *   <tr><td>Predictability</td><td>High (structured)</td><td>Low (emergent)</td></tr>
 *   <tr><td>Performance</td><td>O(m × d)</td><td>O(b^d)</td></tr>
 *   <tr><td>Minecraft</td><td>Excellent fit</td><td>Less suitable</td></tr>
 * </table>
 *
 * <h2>Integration with Action System</h2>
 * <p>HTN primitive tasks map directly to {@link com.minewright.action.Action} types:</p>
 * <ul>
 *   <li>{@code mine} → {@link com.minewright.action.actions.MineBlockAction}</li>
 *   <li>{@code place} → {@link com.minewright.action.actions.PlaceBlockAction}</li>
 *   <li>{@code craft} → {@link com.minewright.action.actions.CraftItemAction}</li>
 *   <li>{@code pathfind} → {@link com.minewright.action.actions.PathfindAction}</li>
 *   <li>{@code build} → {@link com.minewright.action.actions.BuildStructureAction}</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>All HTN classes are thread-safe for concurrent planning operations.
 * Planning calls use immutable snapshots and independent tracking structures.</p>
 *
 * @see com.minewright.action.ActionExecutor
 * @see com.minewright.llm.TaskPlanner
 *
 * @since 1.0.0
 */
package com.minewright.htn;
