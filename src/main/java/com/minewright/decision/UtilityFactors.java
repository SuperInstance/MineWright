package com.minewright.decision;

import com.minewright.action.Task;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Optional;

/**
 * Built-in utility factors for scoring MineWright tasks.
 *
 * <p><b>Purpose:</b></p>
 * <p>This class provides a comprehensive set of pre-built utility factors
 * that cover common decision-making scenarios for autonomous agents.
 * Each factor evaluates a specific aspect of task desirability.</p>
 *
 * <p><b>Available Factors:</b></p>
 * <ul>
 *   <li>{@link #URGENCY} - Time pressure and deadlines</li>
 *   <li>{@link #RESOURCE_PROXIMITY} - Distance to required resources</li>
 *   <li>{@link #SAFETY} - Threat assessment and danger level</li>
 *   <li>{@link #EFFICIENCY} - Expected success rate and time cost</li>
 *   <li>{@link #PLAYER_PREFERENCE} - What player has asked for before</li>
 *   <li>{@link #SKILL_MATCH} - How well skills match the task</li>
 *   <li>{@link #TOOL_READINESS} - Do we have the right tools</li>
 *   <li>{@link #HEALTH_STATUS} - Agent's current health level</li>
 *   <li>{@link #HUNGER_STATUS} - Agent's hunger/saturation level</li>
 *   <li>{@link #TIME_OF_DAY} - Day/night cycle considerations</li>
 *   <li>{@link #WEATHER_CONDITIONS} - Rain/thunder impact</li>
 * </ul>
 *
 * <p><b>Usage:</b></p>
 * <pre>
 * TaskPrioritizer prioritizer = new TaskPrioritizer();
 * prioritizer.addFactor(UtilityFactors.URGENCY);
 * prioritizer.addFactor(UtilityFactors.SAFETY, 2.0); // High weight
 * </pre>
 *
 * @see UtilityFactor
 * @see TaskPrioritizer
 * @since 1.0.0
 */
public final class UtilityFactors {

    private UtilityFactors() {
        // Prevent instantiation
    }

    /**
     * Evaluates time pressure and task urgency.
     *
     * <p>Tasks with deadlines or time-sensitive goals receive higher scores.
     * This helps agents prioritize time-critical actions over less urgent ones.</p>
     *
     * <p><b>Scoring:</b></p>
     * <ul>
     *   <li>1.0 = Deadline within 100 ticks (5 seconds)</li>
     *   <li>0.7 = Deadline within 1000 ticks (50 seconds)</li>
     *   <li>0.5 = No deadline or distant deadline</li>
     *   <li>0.3 = Long-term task, can wait</li>
     * </ul>
     */
    public static final UtilityFactor URGENCY = new UtilityFactor() {
        @Override
        public double calculate(Task task, DecisionContext context) {
            // Check for explicit deadline parameter
            long deadline = task.getIntParameter("deadline", 0);
            if (deadline > 0) {
                long remaining = deadline - context.getGameTime();
                if (remaining < 0) return 1.0; // Overdue - very urgent
                if (remaining < 100) return 0.9; // Very urgent
                if (remaining < 1000) return 0.7; // Moderately urgent
                if (remaining < 5000) return 0.5; // Normal urgency
                return 0.3; // Low urgency - can wait
            }

            // Check action type for implicit urgency
            String action = task.getAction();
            return switch (action) {
                case "attack" -> 0.8; // Combat is urgent
                case "follow" -> 0.6; // Following is moderately urgent
                case "pathfind" -> 0.5; // Neutral
                case "mine", "gather" -> 0.4; // Resource gathering is low urgency
                case "build", "place" -> 0.3; // Building can usually wait
                case "craft" -> 0.5; // Neutral
                default -> 0.5; // Default neutral
            };
        }

        @Override
        public String getName() {
            return "urgency";
        }

        @Override
        public java.util.Optional<String> getDescription() {
            return java.util.Optional.of("Evaluates time pressure and deadlines for tasks");
        }
    };

    /**
     * Evaluates proximity to required resources.
     *
     * <p>Tasks that can be completed with nearby resources score higher.
     * This minimizes travel time and improves efficiency.</p>
     *
     * <p><b>Scoring:</b></p>
     * <ul>
     *   <li>1.0 = Resources within 16 blocks</li>
     *   <li>0.7 = Resources within 64 blocks</li>
     *   <li>0.5 = Resources within 128 blocks</li>
     *   <li>0.3 = Resources far away</li>
     * </ul>
     */
    public static final UtilityFactor RESOURCE_PROXIMITY = new UtilityFactor() {
        @Override
        public double calculate(Task task, DecisionContext context) {
            // For now, use a simplified check based on task type
            // A full implementation would scan the world for actual resources

            BlockPos agentPos = context.getAgentPosition();

            // Check if task specifies a target location
            if (task.hasParameters("x", "y", "z")) {
                int tx = task.getIntParameter("x", 0);
                int ty = task.getIntParameter("y", 0);
                int tz = task.getIntParameter("z", 0);
                BlockPos targetPos = new BlockPos(tx, ty, tz);
                double distance = agentPos.distSqr(targetPos);

                // Convert distance to score (closer = higher)
                if (distance < 256) return 1.0;    // Within 16 blocks
                if (distance < 4096) return 0.7;  // Within 64 blocks
                if (distance < 16384) return 0.5; // Within 128 blocks
                return 0.3; // Far away
            }

            // For tasks without location, check nearby resources
            if (context.getNearbyResources().isEmpty()) {
                return 0.5; // Unknown location, neutral score
            }

            // Find nearest resource
            double nearestDist = context.getNearbyResources().stream()
                .mapToDouble(pos -> agentPos.distSqr(pos))
                .min()
                .orElse(Double.MAX_VALUE);

            if (nearestDist < 256) return 1.0;
            if (nearestDist < 4096) return 0.7;
            if (nearestDist < 16384) return 0.5;
            return 0.3;
        }

        @Override
        public String getName() {
            return "resource_proximity";
        }

        @Override
        public java.util.Optional<String> getDescription() {
            return java.util.Optional.of("Evaluates distance to required resources and task locations");
        }
    };

    /**
     * Evaluates safety based on nearby threats and conditions.
     *
     * <p>Tasks are scored lower if there are nearby threats or dangerous
     * conditions (low health, night time, etc.).</p>
     *
     * <p><b>Scoring:</b></p>
     * <ul>
     *   <li>1.0 = Completely safe (no threats, good health, daytime)</li>
     *   <li>0.7 = Minor threats (1-2 mobs, full health)</li>
     *   <li>0.5 = Moderate risk (3-5 mobs or some damage)</li>
     *   <li>0.3 = High risk (many mobs or low health)</li>
     *   <li>0.0 = Extreme danger (critical health, many threats)</li>
     * </ul>
     */
    public static final UtilityFactor SAFETY = new UtilityFactor() {
        @Override
        public double calculate(Task task, DecisionContext context) {
            double score = 1.0;

            // Reduce score based on health
            double health = context.getHealthLevel();
            if (health < 0.2) score -= 0.5; // Critical health
            else if (health < 0.5) score -= 0.3; // Hurt

            // Reduce score based on nearby threats
            int threatCount = context.getNearbyThreats().size();
            if (threatCount > 5) score -= 0.4;
            else if (threatCount > 3) score -= 0.2;
            else if (threatCount > 0) score -= 0.1;

            // Combat tasks get lower safety score (they're inherently risky)
            if ("attack".equals(task.getAction())) {
                score -= 0.2;
            }

            // Night time is less safe
            if (!context.isDaytime()) {
                score -= 0.1;
            }

            return Math.max(0.0, Math.min(1.0, score));
        }

        @Override
        public double getDefaultWeight() {
            return 1.5; // Safety is important
        }

        @Override
        public String getName() {
            return "safety";
        }

        @Override
        public java.util.Optional<String> getDescription() {
            return java.util.Optional.of("Assesses threat level and danger based on health and nearby hostiles");
        }
    };

    /**
     * Evaluates expected efficiency of task completion.
     *
     * <p>Considers success probability, time cost, and resource requirements
     * to estimate how efficiently the task can be completed.</p>
     *
     * <p><b>Scoring:</b></p>
     * <ul>
     *   <li>1.0 = High efficiency (quick, certain success)</li>
     *   <li>0.7 = Moderate efficiency</li>
     *   <li>0.5 = Average efficiency</li>
     *   <li>0.3 = Low efficiency (slow or risky)</li>
     * </ul>
     */
    public static final UtilityFactor EFFICIENCY = new UtilityFactor() {
        @Override
        public double calculate(Task task, DecisionContext context) {
            String action = task.getAction();

            // Base efficiency by action type
            double baseEfficiency = switch (action) {
                case "pathfind" -> 0.9; // Pathfinding is usually efficient
                case "follow" -> 0.8; // Following is straightforward
                case "mine" -> 0.6; // Mining takes time
                case "place" -> 0.7; // Placing is quick
                case "build" -> 0.5; // Building is complex
                case "craft" -> 0.8; // Crafting is efficient if resources available
                case "attack" -> 0.5; // Combat is variable
                case "gather" -> 0.6; // Gathering takes time
                default -> 0.5; // Unknown - assume average
            };

            // Adjust for quantity if specified
            int quantity = task.getIntParameter("quantity", 1);
            if (quantity > 64) {
                baseEfficiency -= 0.2; // Large jobs are less efficient
            } else if (quantity > 16) {
                baseEfficiency -= 0.1;
            }

            return Math.max(0.0, Math.min(1.0, baseEfficiency));
        }

        @Override
        public String getName() {
            return "efficiency";
        }

        @Override
        public java.util.Optional<String> getDescription() {
            return java.util.Optional.of("Estimates completion efficiency based on task type and scope");
        }
    };

    /**
     * Evaluates player preferences and historical choices.
     *
     * <p>Tasks that align with what the player has previously requested
     * receive higher scores. This enables learning of player preferences.</p>
     *
     * <p><b>Scoring:</b></p>
     * <ul>
     *   <li>1.0 = Frequently requested by player</li>
     *   <li>0.7 = Occasionally requested</li>
     *   <li>0.5 = Neutral / no history</li>
     *   <li>0.3 = Rarely requested or player dislikes</li>
     * </ul>
     */
    public static final UtilityFactor PLAYER_PREFERENCE = new UtilityFactor() {
        @Override
        public double calculate(Task task, DecisionContext context) {
            // Check relationship level - high relationship means player's
            // preferences are more important
            double relationship = context.getRelationshipLevel();

            // For now, use a simple heuristic based on relationship
            // A full implementation would track task type frequency in memory
            return 0.3 + (relationship * 0.4); // Range: 0.3 to 0.7
        }

        @Override
        public String getName() {
            return "player_preference";
        }

        @Override
        public java.util.Optional<String> getDescription() {
            return java.util.Optional.of("Evaluates alignment with player preferences and historical choices");
        }
    };

    /**
     * Evaluates how well the agent's skills match the task requirements.
     *
     * <p>Tasks that the agent is "skilled" at (has done frequently before)
     * receive higher scores. This enables skill development over time.</p>
     *
     * <p><b>Scoring:</b></p>
     * <ul>
     *   <li>1.0 = Expert (done 50+ times)</li>
     *   <li>0.7 = Skilled (done 20+ times)</li>
     *   <li>0.5 = Competent (done 5+ times)</li>
     *   <li>0.3 = Novice (done 0-4 times)</li>
     * </ul>
     */
    public static final UtilityFactor SKILL_MATCH = new UtilityFactor() {
        @Override
        public double calculate(Task task, DecisionContext context) {
            // Check memory for task completion history
            var memory = context.getMemory();
            if (memory == null) {
                return 0.5; // Neutral if no memory
            }

            // Count how many times this action type has been completed
            String action = task.getAction();
            long completionCount = memory.getRecentActions(Integer.MAX_VALUE).stream()
                .filter(a -> a.toLowerCase().contains(action))
                .count();

            // Convert count to skill score
            if (completionCount >= 50) return 1.0; // Expert
            if (completionCount >= 20) return 0.7; // Skilled
            if (completionCount >= 5) return 0.5; // Competent
            return 0.3; // Novice
        }

        @Override
        public String getName() {
            return "skill_match";
        }

        @Override
        public java.util.Optional<String> getDescription() {
            return java.util.Optional.of("Evaluates how well the agent's skills match task requirements");
        }
    };

    /**
     * Evaluates whether the agent has the necessary tools for the task.
     *
     * <p>Tasks that can be completed with available tools score higher.
     * Tasks requiring missing tools receive lower scores.</p>
     *
     * <p><b>Scoring:</b></p>
     * <ul>
     *   <li>1.0 = Has optimal tool (e.g., diamond pickaxe for mining)</li>
     *   <li>0.7 = Has adequate tool</li>
     *   <li>0.5 = Can do without tool (hand)</li>
     *   <li>0.3 = Needs tool, has none</li>
     * </ul>
     */
    public static final UtilityFactor TOOL_READINESS = new UtilityFactor() {
        @Override
        public double calculate(Task task, DecisionContext context) {
            String action = task.getAction();

            // Define required tools for each action type
            String requiredTool = switch (action) {
                case "mine" -> "pickaxe";
                case "gather" -> task.getStringParameter("resource", "")
                    .contains("wood") ? "axe" : "pickaxe";
                case "build", "place" -> "block"; // Not a tool, but needs blocks
                case "craft" -> "crafting";
                default -> null;
            };

            if (requiredTool == null) {
                return 1.0; // No tool required
            }

            // Check if agent has the required tool
            boolean hasTool = context.getAvailableTools().stream()
                .anyMatch(stack -> {
                    if (stack == null || stack.isEmpty()) return false;
                    String itemName = stack.getItem().toString().toLowerCase();
                    return itemName.contains(requiredTool);
                });

            if (hasTool) {
                return 0.9; // Has tool
            } else if ("pickaxe".equals(requiredTool) || "axe".equals(requiredTool)) {
                return 0.5; // Can do by hand (slowly)
            } else {
                return 0.3; // Needs tool, doesn't have it
            }
        }

        @Override
        public boolean appliesTo(Task task) {
            String action = task.getAction();
            return action.equals("mine") || action.equals("gather")
                || action.equals("build") || action.equals("place")
                || action.equals("craft");
        }

        @Override
        public String getName() {
            return "tool_readiness";
        }

        @Override
        public java.util.Optional<String> getDescription() {
            return java.util.Optional.of("Checks if the agent has the necessary tools for the task");
        }
    };

    /**
     * Evaluates the agent's current health status.
     *
     * <p>Tasks may be more or less desirable based on current health.
     * Low health makes risky tasks less desirable.</p>
     *
     * <p><b>Scoring:</b></p>
     * <ul>
     *   <li>1.0 = Full health (>90%)</li>
     *   <li>0.7 = Good health (60-90%)</li>
     *   <li>0.5 = Moderate health (30-60%)</li>
     *   <li>0.3 = Low health (<30%)</li>
     * </ul>
     */
    public static final UtilityFactor HEALTH_STATUS = new UtilityFactor() {
        @Override
        public double calculate(Task task, DecisionContext context) {
            double health = context.getHealthLevel();

            if (health > 0.9) return 1.0;
            if (health > 0.6) return 0.7;
            if (health > 0.3) return 0.5;
            return 0.3;
        }

        @Override
        public double getDefaultWeight() {
            return 0.8; // Important but not critical
        }

        @Override
        public String getName() {
            return "health_status";
        }

        @Override
        public java.util.Optional<String> getDescription() {
            return java.util.Optional.of("Evaluates the agent's current health level");
        }
    };

    /**
     * Evaluates the agent's hunger/saturation status.
     *
     * <p>Low hunger affects performance and should influence task selection.
     * Food-related tasks get higher priority when hungry.</p>
     *
     * <p><b>Scoring:</b></p>
     * <ul>
     *   <li>1.0 = Well fed (>80% saturation)</li>
     *   <li>0.7 = Moderate hunger (50-80%)</li>
     *   <li>0.5 = Hungry (20-50%)</li>
     *   <li>0.3 = Starving (<20%)</li>
     * </ul>
     *
     * <p><b>Special Case:</b> Food gathering tasks get score boost when hungry.</p>
     */
    public static final UtilityFactor HUNGER_STATUS = new UtilityFactor() {
        @Override
        public double calculate(Task task, DecisionContext context) {
            double hunger = context.getHungerLevel();

            // Check if this is a food-related task
            String action = task.getAction();
            boolean isFoodTask = action.equals("gather") &&
                task.getStringParameter("resource", "").toLowerCase().contains("food");

            // Boost food gathering when hungry
            if (isFoodTask && hunger < 0.5) {
                return 1.0; // Very important to get food
            }

            if (hunger > 0.8) return 1.0;
            if (hunger > 0.5) return 0.7;
            if (hunger > 0.2) return 0.5;
            return 0.3;
        }

        @Override
        public String getName() {
            return "hunger_status";
        }

        @Override
        public java.util.Optional<String> getDescription() {
            return java.util.Optional.of("Evaluates the agent's hunger and saturation level");
        }
    };

    /**
     * Evaluates time-of-day considerations for tasks.
     *
     * <p>Certain tasks are better suited for day or night.
     * For example, building is safer during the day.</p>
     *
     * <p><b>Scoring:</b></p>
     * <ul>
     *   <li>Daytime: Building, gathering, exploring = 1.0</li>
     *   <li>Nighttime: Combat, cowering, crafting = 1.0</li>
     *   <li>Inverted: Dangerous tasks at night = 0.3</li>
     * </ul>
     */
    public static final UtilityFactor TIME_OF_DAY = new UtilityFactor() {
        @Override
        public double calculate(Task task, DecisionContext context) {
            boolean isDay = context.isDaytime();
            String action = task.getAction();

            return switch (action) {
                case "build", "place", "gather", "mine" -> isDay ? 0.8 : 0.4; // Better in day
                case "attack" -> !isDay ? 0.7 : 0.5; // Mobs spawn at night
                case "craft" -> 0.7; // Neutral - crafting is fine anytime
                case "pathfind" -> isDay ? 0.7 : 0.5; // Safer in day
                case "follow" -> 0.7; // Neutral
                default -> 0.5; // Default neutral
            };
        }

        @Override
        public String getName() {
            return "time_of_day";
        }

        @Override
        public java.util.Optional<String> getDescription() {
            return java.util.Optional.of("Evaluates whether the task is suitable for current time of day");
        }
    };

    /**
     * Evaluates weather condition impact on tasks.
     *
     * <p>Rain and thunder can make certain tasks less desirable.
     * For example, building in thunder is dangerous.</p>
     *
     * <p><b>Scoring:</b></p>
     * <ul>
     *   <li>Clear weather: All tasks = 1.0</li>
     *   <li>Rain: Outdoor tasks = 0.6, Indoor tasks = 1.0</li>
     *   <li>Thunder: Outdoor tasks = 0.3, Indoor tasks = 0.8</li>
     * </ul>
     */
    public static final UtilityFactor WEATHER_CONDITIONS = new UtilityFactor() {
        @Override
        public double calculate(Task task, DecisionContext context) {
            boolean isRaining = context.isRaining();
            boolean isThundering = context.isThundering();

            if (!isRaining && !isThundering) {
                return 1.0; // Clear weather - perfect
            }

            String action = task.getAction();

            // Some tasks are unaffected by weather
            if (action.equals("craft") || action.equals("pathfind")) {
                return 0.8;
            }

            if (isThundering) {
                // Thunder is dangerous for outdoor activities
                return switch (action) {
                    case "build", "place", "mine", "gather" -> 0.3; // Dangerous
                    case "attack" -> 0.5; // Mobs spawn more in thunder
                    default -> 0.5;
                };
            }

            if (isRaining) {
                // Rain is less severe but still affects outdoor tasks
                return switch (action) {
                    case "build", "place" -> 0.6; // Unpleasant
                    case "mine", "gather" -> 0.7; // Manageable
                    default -> 0.8;
                };
            }

            return 0.8;
        }

        @Override
        public String getName() {
            return "weather_conditions";
        }

        @Override
        public java.util.Optional<String> getDescription() {
            return java.util.Optional.of("Evaluates weather impact on task desirability");
        }
    };
}
