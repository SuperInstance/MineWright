package com.minewright.action;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation for declaring tool parameters within {@link ToolMetadata}.
 *
 * <p>Used to declare the parameters that a tool accepts. These parameters
 * are used to generate the JSON schema for LLM function calling.</p>
 *
 * <h3>Supported Types</h3>
 * <ul>
 *   <li>{@code string} - Text value</li>
 *   <li>{@code integer} - Whole number</li>
 *   <li>{@code number} - Decimal number</li>
 *   <li>{@code boolean} - True/false</li>
 *   <li>{@code array} - List of values</li>
 *   <li>{@code object} - Nested object</li>
 * </ul>
 *
 * <h3>Usage Example</h3>
 * <pre>{@code
 * @ToolMetadata(
 *     name = "mine_block",
 *     description = "Mine blocks of specified type",
 *     parameters = {
 *         @ToolParam(
 *             name = "type",
 *             type = "string",
 *             description = "Block type to mine (e.g., 'stone', 'oak_log')",
 *             required = true
 *         ),
 *         @ToolParam(
 *             name = "radius",
 *             type = "integer",
 *             description = "Search radius in blocks",
 *             required = false,
 *             defaultValue = "5",
 *             minValue = "1",
 *             maxValue = "10"
 *         ),
 *         @ToolParam(
 *             name = "tool",
 *             type = "string",
 *             description = "Tool to use for mining",
 *             required = false,
 *             enumValues = {"pickaxe", "axe", "shovel", "auto"}
 *         )
 *     }
 * )
 * public class MineBlockAction extends BaseAction { }
 * }</pre>
 *
 * @see ToolMetadata
 * @since 1.8.0
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ToolParam {

    /**
     * Parameter name.
     *
     * <p>Should be lowercase with underscores to match OpenAI parameter
     * naming conventions.</p>
     *
     * @return Parameter name
     */
    String name();

    /**
     * Parameter type.
     *
     * <p>JSON Schema types: string, integer, number, boolean, array, object</p>
     *
     * @return JSON Schema type
     */
    String type();

    /**
     * Parameter description.
     *
     * <p>Clear descriptions help LLMs understand what values to provide.
     * Include examples when helpful.</p>
     *
     * @return Parameter description
     */
    String description();

    /**
     * Whether this parameter is required.
     *
     * @return true if required (default: true)
     */
    boolean required() default true;

    /**
     * Default value for optional parameters.
     *
     * <p>Only used if required() is false. The value is a string
     * representation that will be parsed according to the type.</p>
     *
     * @return Default value as string, or empty for no default
     */
    String defaultValue() default "";

    /**
     * Allowed values for enum parameters.
     *
     * <p>If non-empty, the parameter must be one of these values.
     * Useful for parameters with a fixed set of options.</p>
     *
     * @return Array of allowed values
     */
    String[] enumValues() default {};

    /**
     * Minimum value for numeric parameters.
     *
     * <p>Only applicable to integer and number types.</p>
     *
     * @return Minimum value as string, or empty for no minimum
     */
    String minValue() default "";

    /**
     * Maximum value for numeric parameters.
     *
     * <p>Only applicable to integer and number types.</p>
     *
     * @return Maximum value as string, or empty for no maximum
     */
    String maxValue() default "";

    /**
     * Pattern for string parameters.
     *
     * <p>A regex pattern that the value must match.</p>
     *
     * @return Regex pattern, or empty for no pattern
     */
    String pattern() default "";

    /**
     * Example value for documentation.
     *
     * <p>Examples help LLMs understand what values to provide.</p>
     *
     * @return Example value as string
     */
    String example() default "";
}
