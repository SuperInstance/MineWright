package com.minewright.plugin;

import com.minewright.action.actions.BaseAction;
import com.minewright.action.Task;
import com.minewright.entity.ForemanEntity;
import com.minewright.execution.ActionContext;

/**
 * Functional interface for creating action instances.
 *
 * <p>This interface follows the <b>Factory Pattern</b>, allowing dynamic creation
 * of actions based on runtime parameters. Using a functional interface enables
 * clean lambda syntax for registration.</p>
 *
 * <p><b>Example Usage:</b></p>
 * <pre>
 * // Lambda syntax (preferred)
 * ActionFactory mineFactory = (minewright, task, ctx) -&gt;
 *     new MineBlockAction(minewright, task);
 *
 * // Method reference syntax
 * ActionFactory buildFactory = BuildStructureAction::new;
 *
 * // With dependency injection
 * ActionFactory smartFactory = (minewright, task, ctx) -&gt; {
 *     LLMClient client = ctx.getService(LLMClient.class);
 *     return new SmartAction(minewright, task, client);
 * };
 *
 * // Register with registry
 * registry.register("mine", mineFactory);
 * </pre>
 *
 * <p><b>Design Pattern:</b> Factory Method Pattern with functional interface
 * for flexibility and testability.</p>
 *
 * <p><b>Thread Safety:</b> Factory implementations should be thread-safe
 * as they may be called from multiple threads concurrently.</p>
 *
 * @since 1.1.0
 * @see ActionPlugin
 * @see ActionRegistry
 * @see ActionContext
 */
@FunctionalInterface
public interface ActionFactory {

    /**
     * Creates a new action instance.
     *
     * <p>This method is called each time an action of this type needs to be executed.
     * The ActionContext provides access to dependencies and services.</p>
     *
     * <p><b>Implementation Guidelines:</b></p>
     * <ul>
     *   <li>Always return a new instance (no singletons)</li>
     *   <li>Validate task parameters if needed</li>
     *   <li>Use ActionContext for dependency injection</li>
     *   <li>Throw IllegalArgumentException for invalid parameters</li>
     * </ul>
     *
     * <p><b>Example with Validation:</b></p>
     * <pre>
     * ActionFactory factory = (minewright, task, ctx) -&gt; {
     *     if (!task.hasParameters("block", "quantity")) {
     *         throw new IllegalArgumentException("Mine action requires 'block' and 'quantity'");
     *     }
     *     return new MineBlockAction(minewright, task);
     * };
     * </pre>
     *
     * @param minewright   The MineWright entity that will execute this action
     * @param task    The task containing action parameters from LLM
     * @param context Action context providing dependencies and services
     * @return New action instance ready for execution
     * @throws IllegalArgumentException if task parameters are invalid
     */
    BaseAction create(ForemanEntity minewright, Task task, ActionContext context);
}
