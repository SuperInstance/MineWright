package com.minewright.action;

import com.google.gson.JsonObject;

/**
 * Interface for actions that support OpenAI/Anthropic-style function calling.
 *
 * <p>This interface standardizes how actions expose themselves to LLMs through
 * structured function schemas, enabling better tool selection and parameter
 * validation. Compatible with GPT-4, Claude, Gemini, and other function-calling
 * capable models.</p>
 *
 * <h3>Design Philosophy</h3>
 * <p>Research from LlamaIndex and LangChain shows that clear tool descriptions
 * with examples and constraints significantly improve LLM tool selection accuracy
 * (20-30% improvement in benchmark tests). This interface encourages rich
 * metadata for each action.</p>
 *
 * <h3>Usage Example</h3>
 * <pre>{@code
 * public class MineBlockTool implements FunctionCallingTool {
 *     @Override
 *     public String getName() {
 *         return "mine_block";
 *     }
 *
 *     @Override
 *     public String getDescription() {
 *         return "Mine blocks of specified type within a radius.\n" +
 *                "Examples:\n" +
 *                "- mine_block(type=\"stone\", radius=5) -> Mines nearby stone\n" +
 *                "- mine_block(type=\"oak_log\", radius=3) -> Harvests oak trees\n" +
 *                "Constraints:\n" +
 *                "- Maximum radius: 10 blocks\n" +
 *                "- Cannot mine bedrock";
 *     }
 *
 *     @Override
 *     public JsonObject getParameterSchema() {
 *         JsonObject schema = new JsonObject();
 *         schema.addProperty("type", "object");
 *
 *         JsonObject properties = new JsonObject();
 *         JsonObject typeParam = new JsonObject();
 *         typeParam.addProperty("type", "string");
 *         typeParam.addProperty("description", "Block type to mine");
 *         properties.add("type", typeParam);
 *
 *         JsonObject radiusParam = new JsonObject();
 *         radiusParam.addProperty("type", "integer");
 *         radiusParam.addProperty("description", "Search radius (default: 5)");
 *         radiusParam.addProperty("default", 5);
 *         properties.add("radius", radiusParam);
 *
 *         schema.add("properties", properties);
 *
 *         JsonArray required = new JsonArray();
 *         required.add("type");
 *         schema.add("required", required);
 *
 *         return schema;
 *     }
 *
 *     @Override
 *     public ToolResult execute(JsonObject parameters) {
 *         String blockType = parameters.get("type").getAsString();
 *         int radius = parameters.has("radius") ?
 *             parameters.get("radius").getAsInt() : 5;
 *
 *         // Execute mining logic...
 *         return ToolResult.success("Mined 15 stone blocks");
 *     }
 * }
 * }</pre>
 *
 * @see ToolResult
 * @see ToolMetadata
 * @since 1.8.0
 */
public interface FunctionCallingTool {

    /**
     * Returns the unique name of this tool.
     *
     * <p>Tool names should be lowercase with underscores (snake_case) to match
     * OpenAI function calling conventions. Names should be concise but descriptive.</p>
     *
     * <p>Examples: {@code mine_block}, {@code craft_item}, {@code path_to}</p>
     *
     * @return The tool name (e.g., "mine_block")
     */
    String getName();

    /**
     * Returns a detailed description of what this tool does.
     *
     * <p>Research shows that rich descriptions with examples and constraints
     * significantly improve LLM tool selection accuracy. A good description includes:</p>
     *
     * <ul>
     *   <li>What the tool does (first line summary)</li>
     *   <li>Usage examples with expected outputs</li>
     *   <li>Constraints and limitations</li>
     *   <li>Prerequisites (e.g., "Requires iron pickaxe or better")</li>
     * </ul>
     *
     * <p>Example:</p>
     * <pre>
     * Mine blocks of specified type within a radius.
     *
     * Examples:
     * - mine_block(type="stone", radius=5) -> Mines nearby stone
     * - mine_block(type="oak_log", radius=3) -> Harvests oak trees
     *
     * Constraints:
     * - Maximum radius: 10 blocks
     * - Cannot mine bedrock or other unbreakable blocks
     * - Requires tool with sufficient mining level
     * </pre>
     *
     * @return Detailed tool description
     */
    String getDescription();

    /**
     * Returns the JSON Schema for this tool's parameters.
     *
     * <p>The schema follows OpenAI's function calling format:</p>
     * <pre>{@code
     * {
     *   "type": "object",
     *   "properties": {
     *     "param_name": {
     *       "type": "string|integer|boolean|...",
     *       "description": "Parameter description",
     *       "enum": ["option1", "option2"],  // Optional
     *       "default": "default_value"       // Optional
     *     }
     *   },
     *   "required": ["required_param1", "required_param2"]
     * }
     * }</pre>
     *
     * @return JSON Schema object describing parameters
     */
    JsonObject getParameterSchema();

    /**
     * Executes this tool with the given parameters.
     *
     * <p>Parameters are provided as a JsonObject matching the schema from
     * {@link #getParameterSchema()}. The implementation should:</p>
     *
     * <ol>
     *   <li>Validate parameters against the schema</li>
     *   <li>Execute the tool logic</li>
     *   <li>Return a {@link ToolResult} indicating success or failure</li>
     * </ol>
     *
     * @param parameters The parameters for tool execution
     * @return The result of tool execution
     * @throws ToolExecutionException if execution fails unexpectedly
     */
    ToolResult execute(JsonObject parameters) throws ToolExecutionException;

    /**
     * Returns optional examples of tool usage.
     *
     * <p>Examples help LLMs understand how to use the tool correctly.
     * Each example should include the parameters and expected outcome.</p>
     *
     * <p>Default implementation returns empty array. Override to provide
     * specific examples.</p>
     *
     * @return Array of example parameter objects
     */
    default JsonObject[] getExamples() {
        return new JsonObject[0];
    }

    /**
     * Returns the category this tool belongs to.
     *
     * <p>Categories help organize tools in the registry and enable
     * filtered discovery. Common categories:</p>
     *
     * <ul>
     *   <li>{@code mining} - Block breaking and resource extraction</li>
     *   <li>{@code building} - Block placement and construction</li>
     *   <li>{@code crafting} - Item crafting and processing</li>
     *   <li>{@code movement} - Navigation and pathfinding</li>
     *   <li>{@code combat} - Fighting and defense</li>
     *   <li>{@code farming} - Agriculture and food production</li>
     *   <li>{@code utility} - General utility actions</li>
     * </ul>
     *
     * @return Tool category (default: "utility")
     */
    default String getCategory() {
        return "utility";
    }

    /**
     * Returns the estimated tick cost of this tool.
     *
     * <p>This helps the Cascade Router decide whether to use a simple or
     * complex model. Higher cost tools may warrant more capable models.</p>
     *
     * <p>Cost scale:</p>
     * <ul>
     *   <li>1-10: Simple operations (single block, simple movement)</li>
     *   <li>11-50: Moderate operations (multi-block, crafting)</li>
     *   <li>51-100: Complex operations (large structures, combat)</li>
     *   <li>100+: Very complex operations (multi-agent coordination)</li>
     * </ul>
     *
     * @return Estimated tick cost (default: 10)
     */
    default int getEstimatedTickCost() {
        return 10;
    }

    /**
     * Returns whether this tool can be executed in parallel with other tools.
     *
     * <p>Some tools (like pathfinding) should not run concurrently.
     * Default is true for most actions.</p>
     *
     * @return true if parallel execution is safe (default: true)
     */
    default boolean isParallelSafe() {
        return true;
    }
}
