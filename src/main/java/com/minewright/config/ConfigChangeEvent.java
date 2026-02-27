package com.minewright.config;

import java.util.Set;
import java.util.Collections;
import java.util.HashSet;

/**
 * Event containing information about configuration changes.
 *
 * <p>This event is passed to {@link ConfigChangeListener} implementations
 * when configuration values are reloaded.</p>
 *
 * @since 1.5.0
 */
public class ConfigChangeEvent {

    private final Set<String> changedKeys;
    private final boolean fullReload;
    private final int previousVersion;
    private final int newVersion;

    /**
     * Creates a config change event for specific keys that changed.
     *
     * @param changedKeys The config keys that changed (e.g., "ai.provider", "voice.enabled")
     */
    public ConfigChangeEvent(Set<String> changedKeys) {
        this.changedKeys = Collections.unmodifiableSet(new HashSet<>(changedKeys));
        this.fullReload = false;
        this.previousVersion = -1;
        this.newVersion = ConfigVersion.CURRENT_VERSION;
    }

    /**
     * Creates a config change event for a full reload.
     *
     * <p>Use this when the entire configuration has been reloaded
     * and all listeners should refresh their cached values.</p>
     *
     * @param previousVersion The previous config version, or -1 if unknown
     * @param newVersion The new config version
     */
    public ConfigChangeEvent(int previousVersion, int newVersion) {
        this.changedKeys = null;
        this.fullReload = true;
        this.previousVersion = previousVersion;
        this.newVersion = newVersion;
    }

    /**
     * Checks if a specific configuration key changed.
     *
     * <p><b>Examples:</b></p>
     * <ul>
     *   <li>{@code affects("ai")} - true if any AI config changed</li>
     *   <li>{@code affects("ai.provider")} - true if provider changed</li>
     *   <li>{@code affects("voice.enabled")} - true if voice enablement changed</li>
     * </ul>
     *
     * @param key The configuration key to check (can be a prefix)
     * @return true if the key or any of its children changed
     */
    public boolean affects(String key) {
        if (fullReload) {
            return true;
        }

        // Check for exact match
        if (changedKeys.contains(key)) {
            return true;
        }

        // Check for prefix match (e.g., "ai" matches "ai.provider")
        String prefix = key + ".";
        for (String changedKey : changedKeys) {
            if (changedKey.startsWith(prefix)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if this is a full reload event.
     *
     * <p>During a full reload, all configuration values should be refreshed,
     * not just the ones that specifically changed.</p>
     *
     * @return true if this is a full reload
     */
    public boolean isFullReload() {
        return fullReload;
    }

    /**
     * Gets the set of configuration keys that changed.
     *
     * <p>Returns an empty set for full reload events.</p>
     *
     * @return Unmodifiable set of changed keys, or empty set for full reload
     */
    public Set<String> getChangedKeys() {
        return fullReload ? Collections.emptySet() : changedKeys;
    }

    /**
     * Gets the previous configuration version.
     *
     * <p>Returns -1 if the previous version is unknown.</p>
     *
     * @return The previous config version, or -1 if unknown
     */
    public int getPreviousVersion() {
        return previousVersion;
    }

    /**
     * Gets the new configuration version.
     *
     * @return The new config version
     */
    public int getNewVersion() {
        return newVersion;
    }

    @Override
    public String toString() {
        if (fullReload) {
            return "ConfigChangeEvent[fullReload, v" + previousVersion + " -> v" + newVersion + "]";
        }
        return "ConfigChangeEvent[keys=" + changedKeys + "]";
    }
}
