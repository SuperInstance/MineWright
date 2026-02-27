package com.minewright.execution;

import com.minewright.action.actions.BaseAction;
import com.minewright.action.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Chain of interceptors for action execution lifecycle.
 *
 * <p>Manages a collection of ActionInterceptors and invokes them in priority
 * order. Follows the Chain of Responsibility pattern.</p>
 *
 * <p><b>Execution Order:</b></p>
 * <ul>
 *   <li>beforeAction: High priority → Low priority</li>
 *   <li>afterAction: Low priority → High priority (stack unwinding)</li>
 *   <li>onError: Low priority → High priority</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b> Uses CopyOnWriteArrayList for thread-safe iteration.</p>
 *
 * <p><b>Example Usage:</b></p>
 * <pre>
 * InterceptorChain chain = new InterceptorChain();
 * chain.addInterceptor(new LoggingInterceptor());
 * chain.addInterceptor(new MetricsInterceptor());
 *
 * // Before action
 * if (chain.executeBeforeAction(action, context)) {
 *     action.start();
 *     // ... action executes
 *     chain.executeAfterAction(action, result, context);
 * }
 * </pre>
 *
 * @since 1.1.0
 * @see ActionInterceptor
 */
public class InterceptorChain {

    private static final Logger LOGGER = LoggerFactory.getLogger(InterceptorChain.class);

    /**
     * List of interceptors sorted by priority.
     */
    private final CopyOnWriteArrayList<ActionInterceptor> interceptors;

    public InterceptorChain() {
        this.interceptors = new CopyOnWriteArrayList<>();
    }

    /**
     * Adds an interceptor to the chain.
     *
     * <p>The chain is automatically sorted by priority (descending).</p>
     *
     * @param interceptor Interceptor to add
     * @throws IllegalArgumentException if interceptor is null
     */
    public void addInterceptor(ActionInterceptor interceptor) {
        if (interceptor == null) {
            throw new IllegalArgumentException("Interceptor cannot be null");
        }

        interceptors.add(interceptor);
        sortInterceptors();

        LOGGER.debug("Added interceptor: {} (priority: {})",
            interceptor.getName(), interceptor.getPriority());
    }

    /**
     * Removes an interceptor from the chain.
     *
     * @param interceptor Interceptor to remove
     * @return true if removed
     */
    public boolean removeInterceptor(ActionInterceptor interceptor) {
        boolean removed = interceptors.remove(interceptor);
        if (removed) {
            LOGGER.debug("Removed interceptor: {}", interceptor.getName());
        }
        return removed;
    }

    /**
     * Sorts interceptors by priority (descending).
     */
    private void sortInterceptors() {
        List<ActionInterceptor> sorted = new ArrayList<>(interceptors);
        sorted.sort(Comparator.comparingInt(ActionInterceptor::getPriority).reversed());
        interceptors.clear();
        interceptors.addAll(sorted);
    }

    /**
     * Executes all beforeAction interceptors.
     *
     * <p>If any interceptor returns false, execution stops and returns false.</p>
     *
     * @param action  Action about to start
     * @param context Action context
     * @return true if all interceptors approved, false if any rejected
     */
    public boolean executeBeforeAction(BaseAction action, ActionContext context) {
        for (ActionInterceptor interceptor : interceptors) {
            try {
                if (!interceptor.beforeAction(action, context)) {
                    LOGGER.info("Action cancelled by interceptor: {}", interceptor.getName());
                    return false;
                }
            } catch (Exception e) {
                logInterceptorError(interceptor, "beforeAction", e);
            }
        }
        return true;
    }

    /**
     * Executes all afterAction interceptors in reverse priority order.
     *
     * @param action  Completed action
     * @param result  Action result
     * @param context Action context
     */
    public void executeAfterAction(BaseAction action, ActionResult result, ActionContext context) {
        executeInReverseOrder(interceptor ->
            interceptor.afterAction(action, result, context), "afterAction");
    }

    /**
     * Executes all onError interceptors.
     *
     * @param action    Failed action
     * @param exception Exception thrown
     * @param context   Action context
     * @return true if any interceptor suppressed the exception
     */
    public boolean executeOnError(BaseAction action, Exception exception, ActionContext context) {
        final boolean[] suppressed = {false};
        executeInReverseOrder(interceptor -> {
            if (interceptor.onError(action, exception, context)) {
                LOGGER.debug("Exception suppressed by interceptor: {}", interceptor.getName());
                suppressed[0] = true;
            }
        }, "onError");
        return suppressed[0];
    }

    /**
     * Executes interceptors in reverse order with error handling.
     *
     * @param executor Function to execute on each interceptor
     * @param methodName Method name for error logging
     */
    private void executeInReverseOrder(java.util.function.Consumer<ActionInterceptor> executor, String methodName) {
        List<ActionInterceptor> reversed = new ArrayList<>(interceptors);
        Collections.reverse(reversed);

        for (ActionInterceptor interceptor : reversed) {
            try {
                executor.accept(interceptor);
            } catch (Exception e) {
                logInterceptorError(interceptor, methodName, e);
            }
        }
    }

    /**
     * Logs an error from an interceptor with consistent formatting.
     *
     * @param interceptor The interceptor that threw an exception
     * @param methodName The method name where the error occurred
     * @param e The exception
     */
    private void logInterceptorError(ActionInterceptor interceptor, String methodName, Exception e) {
        LOGGER.error("Error in interceptor {} {}: {}",
            interceptor.getName(), methodName, e.getMessage(), e);
    }

    /**
     * Clears all interceptors.
     */
    public void clear() {
        interceptors.clear();
        LOGGER.debug("InterceptorChain cleared");
    }

    /**
     * Returns the number of interceptors.
     *
     * @return Interceptor count
     */
    public int size() {
        return interceptors.size();
    }

    /**
     * Returns an unmodifiable view of interceptors.
     *
     * @return List of interceptors
     */
    public List<ActionInterceptor> getInterceptors() {
        return Collections.unmodifiableList(new ArrayList<>(interceptors));
    }
}
