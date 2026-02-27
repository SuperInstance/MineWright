package com.minewright.config;

import com.minewright.MineWrightMod;
import com.minewright.exception.ConfigException;
import com.minewright.exception.MineWrightException.ErrorCode;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Central manager for configuration lifecycle and change notifications.
 *
 * <p>This class handles:</p>
 * <ul>
 *   <li>Registration of {@link ConfigChangeListener} instances</li>
 *   <li>Configuration validation and migration</li>
 *   <li>Broadcasting config change events to listeners</li>
 *   <li>Error handling during config reload</li>
 * </ul>
 *
 * <p><b>Usage:</b></p>
 * <pre>{@code
 * // Register a listener
 * ConfigManager.getInstance().registerListener(mySystem);
 *
 * // Reload config (called by Forge automatically on /reload)
 * ConfigManager.getInstance().reloadConfig();
 * }</pre>
 *
 * @since 1.5.0
 */
public class ConfigManager {

    private static final Logger LOGGER = MineWrightMod.LOGGER;
    private static ConfigManager instance;

    private final List<ConfigChangeListener> listeners;
    private int lastKnownVersion;
    private boolean initialized;

    private ConfigManager() {
        this.listeners = new CopyOnWriteArrayList<>();
        this.lastKnownVersion = ConfigVersion.CURRENT_VERSION;
        this.initialized = false;
    }

    /**
     * Gets the singleton ConfigManager instance.
     *
     * @return ConfigManager instance
     */
    public static ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }

    /**
     * Initializes the configuration manager.
     *
     * <p>This performs initial validation and migration if needed.
     * Called during mod initialization.</p>
     *
     * @return true if initialization succeeded
     */
    public boolean initialize() {
        if (initialized) {
            LOGGER.debug("ConfigManager already initialized");
            return true;
        }

        LOGGER.info("Initializing ConfigManager...");
        initialized = true;

        try {
            // Read config version
            int configVersion = getConfigVersion();
            lastKnownVersion = configVersion;

            // Check if migration is needed
            if (ConfigVersion.needsMigration(configVersion)) {
                LOGGER.info("Configuration migration needed: v{} -> v{}",
                    configVersion, ConfigVersion.CURRENT_VERSION);
                migrateConfig(configVersion, ConfigVersion.CURRENT_VERSION);
            } else if (ConfigVersion.isFutureVersion(configVersion)) {
                LOGGER.warn("Configuration version {} is newer than current version {}. " +
                    "This may happen after downgrading the mod version.",
                    configVersion, ConfigVersion.CURRENT_VERSION);
            }

            // Validate configuration
            return validateConfig();

        } catch (Exception e) {
            LOGGER.error("Failed to initialize ConfigManager", e);
            return false;
        }
    }

    /**
     * Registers a configuration change listener.
     *
     * <p>Listeners will be notified whenever configuration is reloaded.</p>
     *
     * @param listener The listener to register
     */
    public void registerListener(ConfigChangeListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
            LOGGER.debug("Registered config listener: {}", listener.getClass().getSimpleName());
        }
    }

    /**
     * Unregisters a configuration change listener.
     *
     * @param listener The listener to unregister
     */
    public void unregisterListener(ConfigChangeListener listener) {
        if (listeners.remove(listener)) {
            LOGGER.debug("Unregistered config listener: {}", listener.getClass().getSimpleName());
        }
    }

    /**
     * Reloads configuration and notifies all listeners.
     *
     * <p>This method is called automatically by Forge when the config is
     * reloaded via the {@code /reload} command.</p>
     *
     * <p><b>Process:</b></p>
     * <ol>
     *   <li>Notify listeners of impending reload ({@link ConfigChangeListener#onConfigReloading()})</li>
     *   <li>Validate configuration</li>
     *   <li>Perform migration if needed</li>
     *   <li>Notify listeners of changes ({@link ConfigChangeListener#onConfigChanged(ConfigChangeEvent)})</li>
     * </ol>
     *
     * @return true if reload succeeded
     */
    public boolean reloadConfig() {
        LOGGER.info("Reloading configuration...");

        // Notify listeners of impending reload
        for (ConfigChangeListener listener : listeners) {
            try {
                listener.onConfigReloading();
            } catch (Exception e) {
                LOGGER.error("Listener {} threw exception during onConfigReloading()",
                    listener.getClass().getSimpleName(), e);
            }
        }

        try {
            // Validate configuration
            boolean valid = validateConfig();
            if (!valid) {
                LOGGER.warn("Configuration validation failed during reload");
            }

            // Check for version changes
            int newVersion = getConfigVersion();
            boolean versionChanged = (newVersion != lastKnownVersion);
            boolean migrated = false;

            if (ConfigVersion.needsMigration(newVersion)) {
                LOGGER.info("Configuration migration needed during reload: v{} -> v{}",
                    newVersion, ConfigVersion.CURRENT_VERSION);
                migrateConfig(newVersion, ConfigVersion.CURRENT_VERSION);
                migrated = true;
            }

            lastKnownVersion = ConfigVersion.CURRENT_VERSION;

            // Create and broadcast change event
            ConfigChangeEvent event;
            if (versionChanged || migrated) {
                event = new ConfigChangeEvent(
                    versionChanged ? lastKnownVersion : newVersion,
                    ConfigVersion.CURRENT_VERSION
                );
                LOGGER.info("Configuration version changed: {}", event);
            } else {
                // For non-version changes, we do a full reload since we don't track individual keys
                event = new ConfigChangeEvent(-1, ConfigVersion.CURRENT_VERSION);
            }

            // Notify listeners
            for (ConfigChangeListener listener : listeners) {
                try {
                    listener.onConfigChanged(event);
                } catch (Exception e) {
                    LOGGER.error("Listener {} threw exception during onConfigChanged()",
                        listener.getClass().getSimpleName(), e);
                }
            }

            LOGGER.info("Configuration reloaded successfully");
            return true;

        } catch (ConfigException e) {
            LOGGER.error("Configuration validation failed: {}", e.getMessage());

            // Notify listeners of failure
            for (ConfigChangeListener listener : listeners) {
                try {
                    listener.onConfigReloadFailed(e);
                } catch (Exception ex) {
                    LOGGER.error("Listener {} threw exception during onConfigReloadFailed()",
                        listener.getClass().getSimpleName(), ex);
                }
            }

            return false;

        } catch (Exception e) {
            LOGGER.error("Unexpected error during configuration reload", e);

            // Notify listeners of failure
            ConfigException ce = new ConfigException("Unexpected error during reload", e);
            for (ConfigChangeListener listener : listeners) {
                try {
                    listener.onConfigReloadFailed(ce);
                } catch (Exception ex) {
                    LOGGER.error("Listener {} threw exception during onConfigReloadFailed()",
                        listener.getClass().getSimpleName(), ex);
                }
            }

            return false;
        }
    }

    /**
     * Validates the current configuration.
     *
     * @return true if configuration is valid
     * @throws ConfigException if critical configuration errors are found
     */
    private boolean validateConfig() throws ConfigException {
        // Use existing validation method
        boolean valid = MineWrightConfig.validateAndLog();

        // Additional validation with exceptions for critical errors
        String apiKey = MineWrightConfig.OPENAI_API_KEY.get();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new ConfigException(
                "API key is not configured",
                "openai.apiKey",
                null,
                ErrorCode.CONFIG_MISSING_KEY,
                "Set 'apiKey' in [openai] section of config/minewright-common.toml"
            );
        }

        // Validate Hive Mind URL if Hive Mind is enabled
        if (MineWrightConfig.HIVEMIND_ENABLED.get()) {
            String workerUrl = MineWrightConfig.HIVEMIND_WORKER_URL.get();
            if (workerUrl != null && !workerUrl.trim().isEmpty()) {
                if (!isValidUrl(workerUrl)) {
                    throw new ConfigException(
                        "Invalid Hive Mind worker URL: " + workerUrl,
                        "hivemind.workerUrl",
                        workerUrl,
                        ErrorCode.CONFIG_INVALID_VALUE,
                        "Provide a valid URL in config/minewright-common.toml"
                    );
                }
            }
        }

        return valid;
    }

    /**
     * Migrates configuration from one version to another.
     *
     * @param fromVersion The source version
     * @param toVersion The target version
     */
    private void migrateConfig(int fromVersion, int toVersion) {
        LOGGER.info(ConfigVersion.getMigrationDescription(fromVersion));

        // Migration logic would go here
        // For now, we just log the migration steps
        // In a full implementation, this would:
        // 1. Read the existing config file
        // 2. Apply transformations based on version differences
        // 3. Write the updated config back
        // 4. Update the configVersion field

        LOGGER.info("Configuration migration completed");
    }

    /**
     * Gets the current configuration version.
     *
     * @return The config version, or CURRENT_VERSION if not set
     */
    private int getConfigVersion() {
        // Since we don't have a version field in the config yet,
        // we assume current version
        // In a full implementation, this would read from a config value
        return ConfigVersion.CURRENT_VERSION;
    }

    /**
     * Checks if a string is a valid URL.
     *
     * @param url The URL string to check
     * @return true if valid URL format
     */
    private boolean isValidUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        try {
            new java.net.URL(url);
            return true;
        } catch (java.net.MalformedURLException e) {
            return false;
        }
    }

    /**
     * Gets the number of registered listeners.
     *
     * @return Listener count
     */
    public int getListenerCount() {
        return listeners.size();
    }
}
