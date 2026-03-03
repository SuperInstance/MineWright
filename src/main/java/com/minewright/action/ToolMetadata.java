package com.minewright.action;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for declaring FunctionCallingTool metadata declaratively.
 *
 * <p>This annotation allows action classes to declare their tool metadata
 * (name, description, parameters) without implementing the full
 * {@link FunctionCallingTool} interface. The {@link ActionRegistry} can
 * read this annotation to generate tool schemas for LLM function calling.</p>
 *
 * <h3>Usage Example</h3>
 * <pre>{@code
 * @ToolMetadata(
 *     name = "mine_block",
 *     description = "Mine blocks of specified type within a radius.\n" +
 *                   "Examples:\n" +
 *                   "- mine_block(type=\"stone\", radius=5)\n" +
 *                   "Constraints:\n" +
 *                   "- Maximum radius: 10 blocks",
 *     category = "mining",
 *     parameters = {
 *         @ToolParam(name = "type", type = "string", description = "Block type"),
 *         @ToolParam(name = "radius", type = "integer", required = false, defaultValue = "5")
 *     }
 * )
 * public class MineBlockAction extends BaseAction {
 *     // Action implementation
 * }
 * }</pre>
 *
 * @see ToolParam
 * @see FunctionCallingTool
 * @since 1.8.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ToolMetadata {

    /**
     * The tool name (lowercase with underscores).
     *
     * <p>Should follow OpenAI function naming conventions:
     * lowercase letters, numbers, and underscores only.</p>
     *
     * @return Tool name
     */
    String name();

    /**
     * Detailed description of what this tool does.
     *
     * <p>Research shows that rich descriptions with examples and constraints
     * significantly improve LLM tool selection accuracy (20-30% improvement).</p>
     *
     * @return Tool description
     */
    String description();

    /**
     * The category this tool belongs to.
     *
     * <p>Categories: mining, building, crafting, movement, combat, farming, utility</p>
     *
     * @return Tool category (default: "utility")
     */
    String category() default "utility";

    /**
     * Estimated tick cost (1-100+).
     *
     * <p>Helps Cascade Router select appropriate model complexity.</p>
     *
     * @return Estimated tick cost (default: 10)
     */
    int estimatedTickCost() default 10;

    /**
     * Whether this tool can be executed in parallel with others.
     *
     * @return true if parallel-safe (default: true)
     */
    boolean parallelSafe() default true;

    /**
     * Version of this tool's interface.
     *
     * @return Version string (default: "1.0.0")
     */
    String version() default "1.0.0";

    /**
     * Whether this tool is deprecated.
     *
     * @return true if deprecated (default: false)
     */
    boolean deprecated() default false;

    /**
     * Replacement tool for deprecated tools.
     *
     * @return Name of replacement tool, or empty string
     */
    String replacement() default "";

    /**
     * Parameters for this tool.
     *
     * @return Array of parameter declarations
     */
    ToolParam[] parameters() default {};
}
