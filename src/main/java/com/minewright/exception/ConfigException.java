package com.minewright.exception;

/**
 * Exception thrown when configuration validation fails.
 *
 * <p>Config exceptions can occur due to:
 * <ul>
 *   <li>Missing required configuration values</li>
 *   <li>Invalid configuration values (out of range, wrong type)</li>
 *   <li>Configuration validation failures</li>
 *   <li>Invalid API keys or credentials</li>
 * </ul>
 *
 * <p><b>Recovery:</b> Configuration errors are fixed by:
 * <ul>
 *   <li>Editing the config file (config/minewright-common.toml)</li>
 *   <li>Using /reload to reload configuration</li>
 *   <li>Restarting the server/client</li>
 * </ul>
 *
 * @since 1.2.0
 */
public class ConfigException extends MineWrightException {
    private static final long serialVersionUID = 1L;

    private final String configKey;
    private final String configValue;
    private final boolean critical;

    /**
     * Constructs a new ConfigException.
     *
     * @param message            Error message
     * @param configKey          The configuration key that failed
     * @param configValue        The invalid value (can be null)
     * @param errorCode          Specific error code
     * @param recoverySuggestion Recovery suggestion
     */
    public ConfigException(String message, String configKey, String configValue,
                          ErrorCode errorCode, String recoverySuggestion) {
        this(message, configKey, configValue, errorCode, recoverySuggestion, true);
    }

    /**
     * Constructs a new ConfigException with critical flag.
     *
     * @param message            Error message
     * @param configKey          The configuration key that failed
     * @param configValue        The invalid value (can be null)
     * @param errorCode          Specific error code
     * @param recoverySuggestion Recovery suggestion
     * @param critical           Whether this is a critical error
     */
    public ConfigException(String message, String configKey, String configValue,
                          ErrorCode errorCode, String recoverySuggestion, boolean critical) {
        super(message, errorCode, recoverySuggestion);
        this.configKey = configKey;
        this.configValue = configValue;
        this.critical = critical;
    }

    /**
     * Constructs a new ConfigException with cause.
     *
     * @param message            Error message
     * @param cause              Underlying cause
     */
    public ConfigException(String message, Throwable cause) {
        super(message, ErrorCode.CONFIG_VALIDATION_FAILED, null, null, cause);
        this.configKey = null;
        this.configValue = null;
        this.critical = true;
    }

    /**
     * Returns the configuration key that failed.
     *
     * @return Config key
     */
    public String getConfigKey() {
        return configKey;
    }

    /**
     * Returns the invalid configuration value.
     *
     * @return Config value
     */
    public String getConfigValue() {
        return configValue;
    }

    /**
     * Returns whether this is a critical configuration error.
     *
     * @return true if critical
     */
    public boolean isCritical() {
        return critical;
    }

    /**
     * Returns a detailed error message.
     *
     * @return Detailed message
     */
    public String getDetailedMessage() {
        StringBuilder sb = new StringBuilder(getMessage());
        if (configKey != null) {
            sb.append(" [key: ").append(configKey).append("]");
        }
        if (getRecoverySuggestion() != null) {
            sb.append("\nRecovery: ").append(getRecoverySuggestion());
        }
        return sb.toString();
    }

    // Static factory methods for common errors

    /**
     * Creates an exception for missing required configuration.
     *
     * @param key The missing key
     * @return ConfigException instance
     */
    public static ConfigException missingRequired(String key) {
        return new ConfigException(
            "Missing required configuration: " + key,
            key,
            null,
            ErrorCode.CONFIG_MISSING_KEY,
            "Add the missing key to config/minewright-common.toml",
            true
        );
    }

    /**
     * Creates an exception for invalid enum values.
     *
     * @param key        The config key
     * @param value      The invalid value
     * @param validValues Comma-separated list of valid values
     * @return ConfigException instance
     */
    public static ConfigException invalidEnum(String key, String value, String validValues) {
        return new ConfigException(
            "Invalid value '" + value + "' for '" + key + "'. Valid: " + validValues,
            key,
            value,
            ErrorCode.CONFIG_INVALID_VALUE,
            "Use one of: " + validValues,
            false
        );
    }

    /**
     * Creates an exception for invalid URL values.
     *
     * @param key  The config key
     * @param url  The invalid URL
     * @return ConfigException instance
     */
    public static ConfigException invalidUrl(String key, String url) {
        return new ConfigException(
            "Invalid URL for '" + key + "': " + url,
            key,
            url,
            ErrorCode.CONFIG_INVALID_VALUE,
            "Provide a valid URL",
            false
        );
    }

    /**
     * Creates an exception for missing configuration keys.
     *
     * @param key      The missing key
     * @param section  Config section (e.g., "ai", "voice")
     * @param required Whether this is a required field
     * @return ConfigException instance
     */
    public static ConfigException missingKey(String key, String section, boolean required) {
        String suggestion = required
            ? "Add the missing key to config/minewright-common.toml in section [" + section + "]. " +
              "The mod cannot function without this configuration."
            : "The key is optional. A default value will be used. " +
              "Add it to config/minewright-common.toml in section [" + section + "] to customize.";

        return new ConfigException(
            "Missing configuration key: " + section + "." + key + (required ? " (required)" : " (optional)"),
            section + "." + key,
            null,
            ErrorCode.CONFIG_MISSING_KEY,
            suggestion,
            required
        );
    }

    /**
     * Creates an exception for invalid configuration values.
     *
     * @param key        The config key
     * @param value      The invalid value
     * @param validValues Comma-separated list of valid values
     * @param section    Config section
     * @return ConfigException instance
     */
    public static ConfigException invalidValue(String key, String value, String validValues, String section) {
        return new ConfigException(
            "Invalid value '" + value + "' for key '" + key + "' in section [" + section + "]",
            section + "." + key,
            value,
            ErrorCode.CONFIG_INVALID_VALUE,
            "Valid values are: " + validValues + ". " +
            "Edit config/minewright-common.toml and use /reload to apply changes.",
            false
        );
    }

    /**
     * Creates an exception for API key errors.
     *
     * @param provider The provider (openai, groq, gemini)
     * @return ConfigException instance
     */
    public static ConfigException missingApiKey(String provider) {
        return new ConfigException(
            "API key not configured for provider: " + provider,
            "openai.apiKey",
            null,
            ErrorCode.CONFIG_MISSING_KEY,
            "AI features will not work without an API key. " +
            "Add your " + provider.toUpperCase() + " API key to config/minewright-common.toml:\n" +
            "[openai]\n" +
            "apiKey=\"your-api-key-here\"\n\n" +
            "Then restart the game or use /reload.",
            true
        );
    }

    /**
     * Creates an exception for invalid API key format.
     *
     * @param provider The provider
     * @param keyFormat The detected format issue
     * @return ConfigException instance
     */
    public static ConfigException invalidApiKeyFormat(String provider, String keyFormat) {
        return new ConfigException(
            "Invalid API key format for " + provider + ": " + keyFormat,
            "openai.apiKey",
            "***",
            ErrorCode.CONFIG_INVALID_VALUE,
            "The API key appears to be malformed. " +
            "Ensure it's a valid " + provider.toUpperCase() + " API key. " +
            "Check for extra spaces, quotes, or missing characters in config/minewright-common.toml",
            true
        );
    }

    /**
     * Creates an exception for validation failures.
     *
     * @param reason Why validation failed
     * @param key    The config key that failed validation
     * @return ConfigException instance
     */
    public static ConfigException validationFailed(String key, String reason) {
        return new ConfigException(
            "Configuration validation failed for '" + key + "': " + reason,
            key,
            null,
            ErrorCode.CONFIG_VALIDATION_FAILED,
            "Fix the configuration issue in config/minewright-common.toml and use /reload to apply changes.",
            false
        );
    }

    @Override
    public String toString() {
        return String.format("ConfigException[key='%s', value='%s', critical=%s, code=%d, message='%s']",
            configKey, configValue != null ? configValue : "null", critical, getErrorCode().getCode(), getMessage());
    }
}
