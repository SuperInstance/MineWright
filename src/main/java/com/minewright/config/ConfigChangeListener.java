package com.minewright.config;

import com.minewright.exception.ConfigException;

/**
 * Listener interface for configuration change notifications.
 *
 * <p>Implementations of this interface can register with {@link ConfigManager}
 * to receive notifications when configuration values are reloaded.</p>
 *
 * <p><b>Example Usage:</b></p>
 * <pre>{@code
 * public class MySystemService implements ConfigChangeListener {
 *     private int cachedValue;
 *
 *     @Override
 *     public void onConfigChanged(ConfigChangeEvent event) {
 *         if (event.affects("behavior.actionTickDelay")) {
 *             cachedValue = MineWrightConfig.ACTION_TICK_DELAY.get();
 *             // Reinitialize system with new value
 *         }
 *     }
 *
 *     @Override
 *     public void onConfigReloadFailed(ConfigException exception) {
 *         // Handle reload failure - keep old config or shut down gracefully
 *     }
 * }
 * }</pre>
 *
 * @see ConfigManager
 * @see ConfigChangeEvent
 * @since 1.5.0
 */
@FunctionalInterface
public interface ConfigChangeListener {

    /**
     * Called when configuration values have been reloaded.
     *
     * <p>This method is called after validation passes. Implementations should
     * update any cached values and reconfigure systems as needed.</p>
     *
     * <p><b>Threading:</b> This method is called on the Forge event bus thread.
     * Long-running operations should be dispatched to a worker thread.</p>
     *
     * @param event The config change event containing information about what changed
     */
    void onConfigChanged(ConfigChangeEvent event);

    /**
     * Called when configuration reload fails.
     *
     * <p>Implementations can use this to handle reload failures gracefully,
     * such as keeping the old configuration or shutting down affected systems.</p>
     *
     * <p>The default implementation logs the error and does nothing.</p>
     *
     * @param exception The exception that caused the reload to fail
     */
    default void onConfigReloadFailed(ConfigException exception) {
        // Default: do nothing, caller will log the error
    }

    /**
     * Called before configuration is reloaded.
     *
     * <p>Implementations can use this to prepare for a config change,
     * such as saving state or pausing operations.</p>
     *
     * <p>The default implementation does nothing.</p>
     *
     * <p><b>Threading:</b> This method is called on the Forge event bus thread.</p>
     */
    default void onConfigReloading() {
        // Default: do nothing
    }
}
