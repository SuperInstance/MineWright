package com.minewright.config;

/**
 * Configuration version constants and migration utilities.
 *
 * <p>The configuration version is incremented whenever the config schema
 * changes in a way that requires migration of existing values.</p>
 *
 * <p><b>Version History:</b></p>
 * <ul>
 *   <li><b>1:</b> Initial configuration format</li>
 *   <li><b>2:</b> Added Hive Mind configuration section</li>
 *   <li><b>3:</b> Added voice push-to-talk and timeout settings</li>
 *   <li><b>4:</b> Expanded AI provider options with Gemini support</li>
 *   <li><b>5:</b> Added resilience configuration timeouts and thresholds</li>
 * </ul>
 *
 * @since 1.5.0
 */
public final class ConfigVersion {

    private ConfigVersion() {
        // Utility class
    }

    /**
     * Current configuration version.
     * <p>Increment this when making breaking changes to the config schema.</p>
     */
    public static final int CURRENT_VERSION = 5;

    /**
     * Minimum supported configuration version.
     * <p>Configs older than this will require manual migration.</p>
     */
    public static final int MINIMUM_VERSION = 1;

    /**
     * Configuration key for the version field.
     */
    public static final String VERSION_KEY = "configVersion";

    /**
     * Checks if a config version is supported.
     *
     * @param version The version to check
     * @return true if the version can be migrated to current
     */
    public static boolean isSupported(int version) {
        return version >= MINIMUM_VERSION && version <= CURRENT_VERSION;
    }

    /**
     * Checks if a config version is outdated and needs migration.
     *
     * @param version The version to check
     * @return true if migration is needed
     */
    public static boolean needsMigration(int version) {
        return version < CURRENT_VERSION;
    }

    /**
     * Checks if a config version is from the future (newer than current).
     *
     * <p>This can happen when downgrading the mod version.</p>
     *
     * @param version The version to check
     * @return true if the version is newer than current
     */
    public static boolean isFutureVersion(int version) {
        return version > CURRENT_VERSION;
    }

    /**
     * Gets the migration steps for a given version.
     *
     * <p>Returns a description of what changes were made in each version.</p>
     *
     * @param fromVersion The starting version
     * @return Description of migration steps
     */
    public static String getMigrationDescription(int fromVersion) {
        if (fromVersion >= CURRENT_VERSION) {
            return "No migration needed (already at current version)";
        }

        StringBuilder steps = new StringBuilder();
        steps.append("Migrating configuration from v").append(fromVersion)
             .append(" to v").append(CURRENT_VERSION).append(":\n");

        for (int v = fromVersion; v < CURRENT_VERSION; v++) {
            steps.append("  v").append(v).append(" -> v").append(v + 1).append(": ");
            steps.append(getVersionChangeDescription(v + 1)).append("\n");
        }

        return steps.toString();
    }

    private static String getVersionChangeDescription(int version) {
        return switch (version) {
            case 2 -> "Added Hive Mind configuration section for Cloudflare Edge integration";
            case 3 -> "Added voice push-to-talk key and listening timeout settings";
            case 4 -> "Expanded AI provider options to include Gemini alongside Groq and OpenAI";
            case 5 -> "Added resilience configuration with configurable timeouts and thresholds";
            default -> "Various configuration updates";
        };
    }
}
