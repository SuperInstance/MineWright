package com.minewright.htn;

import java.util.List;
import java.util.logging.Logger;

/**
 * Example demonstrating HTN planning usage for Minecraft tasks.
 *
 * <p>This example shows how to:</p>
 * <ul>
 *   <li>Create an HTN domain with task methods</li>
 *   <li>Define world state with properties</li>
 *   <li>Decompose compound tasks into primitive actions</li>
 *   <li>Convert primitive tasks to executable actions</li>
 * </ul>
 *
 * <h2>Example: Building a House</h2>
 * <pre>{@code
 * // 1. Create planner with default domain
 * HTNPlanner planner = new HTNPlanner(HTNDomain.createDefault());
 *
 * // 2. Define world state
 * HTNWorldState state = HTNWorldState.builder()
 *     .property("hasWood", true)
 *     .property("woodCount", 64)
 *     .property("hasAxe", true)
 *     .build();
 *
 * // 3. Create compound task
 * HTNTask buildHouse = HTNTask.compound("build_house")
 *     .parameter("material", "oak_planks")
 *     .parameter("width", 5)
 *     .build();
 *
 * // 4. Decompose
 * List<HTNTask> primitiveTasks = planner.decompose(buildHouse, state);
 *
 * // 5. Convert to actions
 * for (HTNTask task : primitiveTasks) {
 *     Task action = task.toActionTask();
 *     executor.queueTask(action);
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public class HTNExample {
    private static final Logger LOGGER = Logger.getLogger(HTNExample.class.getName());

    /**
     * Demonstrates basic HTN planning for a simple gathering task.
     */
    public static void basicGatheringExample() {
        LOGGER.info("=== Basic HTN Gathering Example ===");

        // Create planner with default Minecraft domain
        HTNPlanner planner = new HTNPlanner(HTNDomain.createDefault());

        // Define world state - agent has an axe but no wood
        HTNWorldState state = HTNWorldState.builder()
            .property("hasAxe", true)
            .property("hasWood", false)
            .property("woodCount", 0)
            .build();

        // Create compound task for gathering wood
        HTNTask gatherWood = HTNTask.compound("gather_wood")
            .parameter("count", 64)
            .build();

        // Decompose the compound task
        List<HTNTask> primitiveTasks = planner.decompose(gatherWood, state);

        if (primitiveTasks != null) {
            LOGGER.info("Decomposition SUCCESS! Generated " + primitiveTasks.size() + " primitive tasks:");
            for (int i = 0; i < primitiveTasks.size(); i++) {
                HTNTask task = primitiveTasks.get(i);
                LOGGER.info("  " + (i + 1) + ". " + task.getName() + " " + task.getParameters());
            }
        } else {
            LOGGER.warning("Decomposition FAILED - no applicable methods found");
        }
    }

    /**
     * Demonstrates HTN planning with fallback methods.
     * Shows how the planner selects different methods based on world state.
     */
    public static void methodSelectionExample() {
        LOGGER.info("\n=== HTN Method Selection Example ===");

        HTNPlanner planner = new HTNPlanner(HTNDomain.createDefault());

        // Scenario 1: Agent has materials (should use high-priority method)
        LOGGER.info("\n--- Scenario 1: Has Materials ---");
        HTNWorldState withMaterials = HTNWorldState.builder()
            .property("hasMaterials", true)
            .property("hasWood", true)
            .build();

        HTNTask buildHouse = HTNTask.compound("build_house")
            .parameter("material", "oak_planks")
            .build();

        List<HTNTask> result1 = planner.decompose(buildHouse, withMaterials);
        if (result1 != null) {
            LOGGER.info("Decomposition with materials: " + result1.size() + " tasks");
        }

        // Scenario 2: Agent lacks materials (should use fallback method with gathering)
        LOGGER.info("\n--- Scenario 2: Needs Gathering ---");
        HTNWorldState withoutMaterials = HTNWorldState.builder()
            .property("hasMaterials", false)
            .build();

        List<HTNTask> result2 = planner.decompose(buildHouse, withoutMaterials);
        if (result2 != null) {
            LOGGER.info("Decomposition with gathering: " + result2.size() + " tasks");
        }
    }

    /**
     * Demonstrates custom method registration.
     */
    public static void customMethodExample() {
        LOGGER.info("\n=== Custom HTN Method Example ===");

        // Create domain
        HTNDomain domain = HTNDomain.createDefault();

        // Add custom method for building with stone
        domain.addMethod(HTNMethod.builder("build_stone_house", "build_house")
            .description("Build house with stone materials")
            .precondition(state -> state.getBoolean("hasStone"))
            .subtask(HTNTask.primitive("pathfind")
                .parameter("target", "quarry")
                .build())
            .subtask(HTNTask.primitive("mine")
                .parameter("blockType", "stone")
                .parameter("count", 192)
                .build())
            .subtask(HTNTask.primitive("pathfind")
                .parameter("target", "build_site")
                .build())
            .subtask(HTNTask.primitive("build")
                .parameter("material", "stone")
                .build())
            .priority(90) // Between wood methods (100 and 50)
            .build());

        HTNPlanner planner = new HTNPlanner(domain);

        HTNWorldState stoneState = HTNWorldState.builder()
            .property("hasStone", true)
            .build();

        HTNTask buildHouse = HTNTask.compound("build_house").build();
        List<HTNTask> result = planner.decompose(buildHouse, stoneState);

        if (result != null) {
            LOGGER.info("Custom stone house plan: " + result.size() + " tasks");
        }
    }

    /**
     * Demonstrates world state queries before planning.
     */
    public static void planningQueriesExample() {
        LOGGER.info("\n=== HTN Planning Queries Example ===");

        HTNPlanner planner = new HTNPlanner(HTNDomain.createDefault());
        HTNWorldState state = HTNWorldState.builder()
            .property("hasAxe", true)
            .build();

        HTNTask buildHouse = HTNTask.compound("build_house").build();

        // Query: Can this task be decomposed?
        boolean canDecompose = planner.canDecompose(buildHouse, state);
        LOGGER.info("Can decompose 'build_house': " + canDecompose);

        // Query: What methods are applicable?
        List<HTNMethod> methods = planner.getApplicableMethods(buildHouse, state);
        LOGGER.info("Applicable methods: " + methods.size());
        for (HTNMethod method : methods) {
            LOGGER.info("  - " + method.getMethodName() +
                       " (priority=" + method.getPriority() + ")");
        }
    }

    /**
     * Demonstrates integration with the action system.
     */
    public static void actionSystemIntegrationExample() {
        LOGGER.info("\n=== HTN to Action System Integration ===");

        HTNPlanner planner = new HTNPlanner(HTNDomain.createDefault());
        HTNWorldState state = HTNWorldState.builder()
            .property("hasAxe", true)
            .build();

        HTNTask gatherWood = HTNTask.compound("gather_wood")
            .parameter("count", 32)
            .build();

        List<HTNTask> primitiveTasks = planner.decompose(gatherWood, state);

        if (primitiveTasks != null) {
            LOGGER.info("Converting " + primitiveTasks.size() + " HTN tasks to actions:");

            for (HTNTask htnTask : primitiveTasks) {
                if (htnTask.isPrimitive()) {
                    try {
                        com.minewright.action.Task actionTask = htnTask.toActionTask();
                        LOGGER.info("  Action: " + actionTask.getAction() +
                                   " with " + actionTask.getParameters());
                    } catch (Exception e) {
                        LOGGER.warning("  Failed to convert task: " + htnTask.getName());
                    }
                }
            }
        }
    }

    /**
     * Main entry point for running all examples.
     */
    public static void main(String[] args) {
        LOGGER.info("HTN Planning Examples for Steve AI Minecraft Mod");
        LOGGER.info("================================================\n");

        basicGatheringExample();
        methodSelectionExample();
        customMethodExample();
        planningQueriesExample();
        actionSystemIntegrationExample();

        LOGGER.info("\n================================================");
        LOGGER.info("All examples completed!");
    }
}
