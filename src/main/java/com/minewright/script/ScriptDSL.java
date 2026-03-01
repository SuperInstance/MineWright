package com.minewright.script;

import java.util.*;

/**
 * Defines the grammar and schema for the MineWright Script DSL.
 *
 * <p><b>DSL Overview:</b></p>
 * <p>The MineWright Script DSL is a JSON-based domain-specific language for defining
 * automation patterns for AI agents. It provides a declarative way to specify behaviors
 * that can be executed in real-time without LLM intervention.</p>
 *
 * <p><b>JSON Schema:</b></p>
 * <pre>
 * {
 *   "id": "string (unique identifier)",
 *   "name": "string (human-readable name)",
 *   "description": "string (what this script does)",
 *   "version": "string (semantic version)",
 *   "triggers": [
 *     {
 *       "type": "EVENT|CONDITION|TIME|PLAYER_ACTION",
 *       "condition": "expression string",
 *       "delay": "number (ticks, optional)",
 *       "cooldown": "number (ticks, optional)"
 *     }
 *   ],
 *   "variables": {
 *     "varName": "value (string|number|boolean)"
 *   },
 *   "actions": [
 *     {
 *       "type": "SEQUENCE|PARALLEL|CONDITIONAL|LOOP|ATOMIC",
 *       "command": "string (for ATOMIC actions)",
 *       "parameters": {
 *         "key": "value"
 *       },
 *       "condition": "expression (for CONDITIONAL)",
 *       "iterations": "number (for LOOP)",
 *       "children": [ ...nested actions... ]
 *     }
 *   ]
 * }
 * </pre>
 *
 * <p><b>Trigger Types:</b></p>
 * <ul>
 *   <li><b>EVENT:</b> Fired when a game event occurs (e.g., block broken, entity spawned)</li>
 *   <li><b>CONDITION:</b> Evaluated continuously, fires when condition becomes true</li>
 *   <li><b>TIME:</b> Fired at regular intervals (e.g., every 100 ticks)</li>
 *   <li><b>PLAYER_ACTION:</b> Fired when player performs specific action</li>
 * </ul>
 *
 * <p><b>Action Types:</b></p>
 * <ul>
 *   <li><b>SEQUENCE:</b> Execute children in order, all must succeed</li>
 *   <li><b>PARALLEL:</b> Execute all children simultaneously</li>
 *   <li><b>CONDITIONAL:</b> Execute children if condition is true</li>
 *   <li><b>LOOP:</b> Repeat children N times</li>
 *   <li><b>ATOMIC:</b> Execute a single game action (mine, place, move, etc.)</li>
 * </ul>
 *
 * <p><b>Condition Expressions:</b></p>
 * <p>Conditions support simple boolean expressions with variables:</p>
 * <pre>
 *   inventory_count("oak_log") > 10
 *   distance_to_player() < 20
 *   health_percent() < 50
 *   $var_name == "value"
 *   time_of_day() == "day"
 * </pre>
 *
 * <p><b>Variables:</b></p>
 * <p>Scripts can maintain state through variables. Variables are prefixed with $ in conditions:</p>
 * <pre>
 *   "variables": {
 *     "target_block": "oak_log",
 *     "min_quantity": 10,
 *     "return_to_base": false
 *   }
 * </pre>
 *
 * <p><b>Validation Rules:</b></p>
 * <ol>
 *   <li>Script must have unique ID</li>
 *   <li>At least one trigger or action must be defined</li>
 *   <li>Actions must form valid tree structure (no cycles)</li>
 *   <li>Atomic actions must have valid command</li>
 *   <li>Loop iterations must be positive integer</li>
 *   <li>Condition expressions must be syntactically valid</li>
 *   <li>Variable references in conditions must be defined</li>
 *   <li>Maximum depth: 20 levels</li>
 *   <li>Maximum total nodes: 500</li>
 * </ol>
 *
 * <p><b>Example Script:</b></p>
 * <pre>
 * {
 *   "id": "auto_mine_oak",
 *   "name": "Auto Mine Oak Logs",
 *   "description": "Automatically mines oak logs when inventory is empty",
 *   "version": "1.0.0",
 *   "triggers": [
 *     {
 *       "type": "CONDITION",
 *       "condition": "inventory_count(\"oak_log\") < 5",
 *       "cooldown": 100
 *     }
 *   ],
 *   "variables": {
 *     "target_block": "oak_log",
 *     "min_quantity": 10
 *   },
 *   "actions": [
 *     {
 *       "type": "SEQUENCE",
 *       "children": [
 *         {
 *           "type": "CONDITIONAL",
 *           "condition": "distance_to_nearest(\"oak_log\") < 50",
 *           "children": [
 *             {
 *               "type": "ATOMIC",
 *               "command": "pathfind",
 *               "parameters": {
 *                 "target": "nearest",
 *                 "block_type": "$target_block"
 *               }
 *             },
 *             {
 *               "type": "LOOP",
 *               "iterations": "$min_quantity",
 *               "children": [
 *                 {
 *                   "type": "ATOMIC",
 *                   "command": "mine",
 *                   "parameters": {
 *                     "block": "$target_block"
 *                   }
 *                 }
 *               ]
 *             }
 *           ]
 *         }
 *       ]
 *     }
 *   ]
 * }
 * </pre>
 *
 * @see Script
 * @see Trigger
 * @see Action
 * @see ScriptParser
 * @see ScriptValidator
 * @since 1.3.0
 */
public class ScriptDSL {

    /**
     * Supported trigger types in the DSL.
     */
    public enum TriggerType {
        /**
         * Fired when a specific game event occurs.
         * Examples: block_broken, block_placed, entity_spawned, item_dropped
         */
        EVENT,

        /**
         * Continuously evaluated, fires when condition becomes true.
         * Used for reactive behaviors based on world state.
         */
        CONDITION,

        /**
         * Fired at regular time intervals.
         * Specified with a period parameter (in ticks).
         */
        TIME,

        /**
         * Fired when the player performs a specific action.
         * Examples: player_broke_block, player_placed_block, player_used_item
         */
        PLAYER_ACTION
    }

    /**
     * Supported action types in the DSL.
     */
    public enum ActionType {
        /**
         * Execute children in sequential order.
         * All children must succeed for sequence to succeed.
         * Stops at first failure.
         */
        SEQUENCE,

        /**
         * Execute all children simultaneously.
         * Succeeds if all children succeed.
         * Fails if any child fails.
         */
        PARALLEL,

        /**
         * Execute children only if condition is true.
         * Supports optional else branch.
         */
        CONDITIONAL,

        /**
         * Repeat children a specified number of times.
         * Iterations can be static or variable reference.
         */
        LOOP,

        /**
         * Execute a single atomic game action.
         * These are the leaf nodes that interact with the game.
         */
        ATOMIC
    }

    /**
     * Built-in functions available in condition expressions.
     */
    public enum BuiltinFunction {
        // Inventory functions
        INVENTORY_COUNT("inventory_count", "Returns count of item in inventory"),
        INVENTORY_HAS("inventory_has", "Returns true if item is in inventory"),
        INVENTORY_SPACE("inventory_space", "Returns available inventory slots"),

        // Distance/Position functions
        DISTANCE_TO("distance_to", "Returns distance to entity or position"),
        DISTANCE_TO_NEAREST("distance_to_nearest", "Returns distance to nearest block type"),
        DISTANCE_TO_PLAYER("distance_to_player", "Returns distance to player"),

        // Entity state functions
        HEALTH_PERCENT("health_percent", "Returns entity health as percentage"),
        HUNGER_LEVEL("hunger_level", "Returns entity hunger level"),
        IS_ON_GROUND("is_on_ground", "Returns true if entity is on ground"),

        // World state functions
        TIME_OF_DAY("time_of_day", "Returns current time of day (day/night/dusk/dawn)"),
        IS_RAINING("is_raining", "Returns true if currently raining"),
        BLOCK_AT("block_at", "Returns block type at position"),
        BLOCKS_IN_RANGE("blocks_in_range", "Returns count of blocks in range"),

        // Task state functions
        IS_EXECUTING("is_executing", "Returns true if currently executing task"),
        LAST_TASK_SUCCEEDED("last_task_succeeded", "Returns true if last task succeeded"),

        // Variable functions
        VAR_EXISTS("var_exists", "Returns true if variable is defined"),
        VAR_EQUALS("var_equals", "Returns true if variable equals value");

        private final String functionName;
        private final String description;

        BuiltinFunction(String functionName, String description) {
            this.functionName = functionName;
            this.description = description;
        }

        public String getFunctionName() {
            return functionName;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Valid atomic commands that can be executed.
     */
    public enum AtomicCommand {
        // Block interaction
        MINE("mine", "Break a block at target location"),
        PLACE("place", "Place a block at target location"),
        BUILD("build", "Build a structure from template"),

        // Movement
        MOVE("move", "Move to target position"),
        PATHFIND("pathfind", "Calculate and follow path to target"),
        FOLLOW("follow", "Follow an entity"),

        // Inventory
        CRAFT("craft", "Craft an item"),
        GATHER("gather", "Gather resources from nearby"),
        DEPOSIT("deposit", "Deposit items into container"),
        WITHDRAW("withdraw", "Withdraw items from container"),
        EQUIP("equip", "Equip an item"),

        // Combat
        ATTACK("attack", "Attack a target entity"),
        DEFEND("defend", "Defend against attackers"),

        // Communication
        SAY("say", "Send a chat message"),
        NOTIFY("notify", "Send notification to player"),

        // Utility
        LOOK("look", "Look at target position"),
        WAIT("wait", "Wait for specified duration"),
        IDLE("idle", "Enter idle state");

        private final String commandName;
        private final String description;

        AtomicCommand(String commandName, String description) {
            this.commandName = commandName;
            this.description = description;
        }

        public String getCommandName() {
            return commandName;
        }

        public String getDescription() {
            return description;
        }

        /**
         * Finds command by name (case-insensitive).
         */
        public static Optional<AtomicCommand> fromString(String name) {
            for (AtomicCommand cmd : values()) {
                if (cmd.commandName.equalsIgnoreCase(name)) {
                    return Optional.of(cmd);
                }
            }
            return Optional.empty();
        }
    }

    /**
     * DSL schema definition for validation.
     */
    public static class Schema {
        private static final int MAX_DEPTH = 20;
        private static final int MAX_NODES = 500;
        private static final int MAX_LOOP_ITERATIONS = 1000;
        private static final int MAX_STRING_LENGTH = 1000;

        /**
         * Validates if a value conforms to schema expectations.
         */
        public static boolean isValidValueType(Object value) {
            return value instanceof String ||
                   value instanceof Number ||
                   value instanceof Boolean;
        }

        /**
         * Validates if a string length is within limits.
         */
        public static boolean isValidStringLength(String str) {
            return str == null || str.length() <= MAX_STRING_LENGTH;
        }

        /**
         * Validates if a number is within valid range for iterations.
         */
        public static boolean isValidIterationCount(Object value) {
            if (value instanceof Number) {
                int count = ((Number) value).intValue();
                return count > 0 && count <= MAX_LOOP_ITERATIONS;
            }
            return false;
        }

        /**
         * Gets maximum allowed depth for action tree.
         */
        public static int getMaxDepth() {
            return MAX_DEPTH;
        }

        /**
         * Gets maximum allowed nodes in script.
         */
        public static int getMaxNodes() {
            return MAX_NODES;
        }

        /**
         * Gets maximum allowed loop iterations.
         */
        public static int getMaxLoopIterations() {
            return MAX_LOOP_ITERATIONS;
        }

        /**
         * Gets maximum allowed string length.
         */
        public static int getMaxLength() {
            return MAX_STRING_LENGTH;
        }
    }

    /**
     * Reserved words that cannot be used as variable names.
     */
    public static final Set<String> RESERVED_WORDS = Set.of(
        "true", "false", "null",
        "if", "else", "then", "while", "for", "do",
        "break", "continue", "return",
        "and", "or", "not",
        "function", "end"
    );

    /**
     * Valid operators for condition expressions.
     */
    public static final Set<String> VALID_OPERATORS = Set.of(
        "==", "!=", "<", "<=", ">", ">=",
        "and", "or", "not", "&&", "||", "!"
    );

    /**
     * Validates if a variable name is valid.
     */
    public static boolean isValidVariableName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }

        if (RESERVED_WORDS.contains(name)) {
            return false;
        }

        // Must start with letter or underscore
        if (!Character.isLetter(name.charAt(0)) && name.charAt(0) != '_') {
            return false;
        }

        // Remaining characters must be alphanumeric or underscore
        for (int i = 1; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '_') {
                return false;
            }
        }

        return true;
    }

    /**
     * Validates if a condition expression is syntactically valid.
     * This is a basic syntax check - full validation requires parsing.
     */
    public static boolean isValidConditionExpression(String expression) {
        if (expression == null || expression.isEmpty()) {
            return false;
        }

        // Check for balanced parentheses
        int depth = 0;
        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
                if (depth < 0) {
                    return false; // Unbalanced
                }
            }
        }

        return depth == 0;
    }

    /**
     * Creates a minimal valid script template.
     */
    public static Map<String, Object> createTemplate() {
        Map<String, Object> template = new LinkedHashMap<>();

        template.put("id", "script_template");
        template.put("name", "Script Template");
        template.put("description", "A template for creating scripts");
        template.put("version", "1.0.0");

        template.put("triggers", new ArrayList<>());
        template.put("variables", new LinkedHashMap<>());
        template.put("actions", new ArrayList<>());

        return template;
    }
}
