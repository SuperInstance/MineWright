package com.minewright.exception;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for ConfigException.
 * Tests exception construction, factory methods, critical flag, and edge cases.
 */
@DisplayName("ConfigException Tests")
class ConfigExceptionTest {

    // ========== Constructor Tests ==========

    @Test
    @DisplayName("Basic constructor creates exception with config key and value")
    void testBasicConstructor() {
        ConfigException exception = new ConfigException(
            "Invalid configuration",
            "api.endpoint",
            "http://invalid-url",
            MineWrightException.ErrorCode.CONFIG_INVALID_VALUE,
            "Fix the URL"
        );

        assertEquals("Invalid configuration", exception.getMessage());
        assertEquals("api.endpoint", exception.getConfigKey());
        assertEquals("http://invalid-url", exception.getConfigValue());
        assertEquals(MineWrightException.ErrorCode.CONFIG_INVALID_VALUE, exception.getErrorCode());
        assertEquals("Fix the URL", exception.getRecoverySuggestion());
        assertTrue(exception.isCritical(), "Basic constructor should default to critical=true");
        assertNull(exception.getCause());
    }

    @Test
    @DisplayName("Constructor with critical flag allows non-critical exceptions")
    void testConstructorWithCriticalFlag() {
        ConfigException exception = new ConfigException(
            "Optional value missing",
            "optional.theme",
            null,
            MineWrightException.ErrorCode.CONFIG_MISSING_KEY,
            "Use default theme",
            false  // critical = false
        );

        assertEquals("Optional value missing", exception.getMessage());
        assertEquals("optional.theme", exception.getConfigKey());
        assertNull(exception.getConfigValue());
        assertFalse(exception.isCritical(), "Should be non-critical");
    }

    @Test
    @DisplayName("Constructor with only message and cause")
    void testConstructorWithMessageAndCause() {
        Throwable cause = new RuntimeException("Parse error");
        ConfigException exception = new ConfigException(
            "Failed to parse config",
            cause
        );

        assertEquals("Failed to parse config", exception.getMessage());
        assertSame(cause, exception.getCause());
        assertTrue(exception.isCritical(), "Message+cause constructor should default to critical=true");
        assertNull(exception.getConfigKey());
        assertNull(exception.getConfigValue());
        assertEquals(MineWrightException.ErrorCode.CONFIG_VALIDATION_FAILED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Constructor accepts null config value")
    void testConstructorWithNullValue() {
        ConfigException exception = new ConfigException(
            "Missing value",
            "missing.key",
            null,
            MineWrightException.ErrorCode.CONFIG_MISSING_KEY,
            "Add the key"
        );

        assertNull(exception.getConfigValue());
    }

    // ========== Factory Method Tests ==========

    @Test
    @DisplayName("missingRequired factory creates critical exception")
    void testMissingRequiredFactory() {
        ConfigException exception = ConfigException.missingRequired("openai.apiKey");

        assertEquals("Missing required configuration: openai.apiKey", exception.getMessage());
        assertEquals("openai.apiKey", exception.getConfigKey());
        assertNull(exception.getConfigValue());
        assertEquals(MineWrightException.ErrorCode.CONFIG_MISSING_KEY, exception.getErrorCode());
        assertTrue(exception.isCritical(), "Missing required should be critical");
        assertTrue(exception.getRecoverySuggestion().contains("config/minewright-common.toml"));
    }

    @Test
    @DisplayName("invalidEnum factory creates non-critical exception")
    void testInvalidEnumFactory() {
        ConfigException exception = ConfigException.invalidEnum("log.level", "DEBUGGING", "TRACE, DEBUG, INFO, WARN, ERROR");

        assertEquals("Invalid value 'DEBUGGING' for 'log.level'. Valid: TRACE, DEBUG, INFO, WARN, ERROR",
            exception.getMessage());
        assertEquals("log.level", exception.getConfigKey());
        assertEquals("DEBUGGING", exception.getConfigValue());
        assertEquals(MineWrightException.ErrorCode.CONFIG_INVALID_VALUE, exception.getErrorCode());
        assertFalse(exception.isCritical(), "Invalid enum should be non-critical");
        assertTrue(exception.getRecoverySuggestion().contains("TRACE, DEBUG, INFO, WARN, ERROR"));
    }

    @Test
    @DisplayName("invalidUrl factory creates non-critical exception")
    void testInvalidUrlFactory() {
        ConfigException exception = ConfigException.invalidUrl("api.endpoint", "not-a-url");

        assertTrue(exception.getMessage().contains("Invalid URL"));
        assertTrue(exception.getMessage().contains("api.endpoint"));
        assertTrue(exception.getMessage().contains("not-a-url"));
        assertEquals("api.endpoint", exception.getConfigKey());
        assertEquals("not-a-url", exception.getConfigValue());
        assertFalse(exception.isCritical());
        assertTrue(exception.getRecoverySuggestion().contains("valid URL"));
    }

    @Test
    @DisplayName("missingKey factory creates required exception as critical")
    void testMissingKeyFactoryRequired() {
        ConfigException exception = ConfigException.missingKey("apiKey", "openai", true);

        assertTrue(exception.getMessage().contains("openai.apiKey"));
        assertTrue(exception.getMessage().contains("required"));
        assertEquals(MineWrightException.ErrorCode.CONFIG_MISSING_KEY, exception.getErrorCode());
        assertTrue(exception.isCritical(), "Required keys should be critical");
        assertTrue(exception.getRecoverySuggestion().contains("cannot function"));
    }

    @Test
    @DisplayName("missingKey factory creates optional exception as non-critical")
    void testMissingKeyFactoryOptional() {
        ConfigException exception = ConfigException.missingKey("theme", "ui", false);

        assertTrue(exception.getMessage().contains("ui.theme"));
        assertTrue(exception.getMessage().contains("optional"));
        assertEquals(MineWrightException.ErrorCode.CONFIG_MISSING_KEY, exception.getErrorCode());
        assertFalse(exception.isCritical(), "Optional keys should be non-critical");
        assertTrue(exception.getRecoverySuggestion().contains("default value"));
    }

    @Test
    @DisplayName("invalidValue factory creates non-critical exception")
    void testInvalidValueFactory() {
        ConfigException exception = ConfigException.invalidValue(
            "maxRetries",
            "-1",
            "1, 2, 3, 5, 10",
            "retry"
        );

        assertTrue(exception.getMessage().contains("maxRetries"));
        assertTrue(exception.getMessage().contains("-1"));
        assertTrue(exception.getMessage().contains("retry"));
        assertEquals("retry.maxRetries", exception.getConfigKey());
        assertEquals("-1", exception.getConfigValue());
        assertFalse(exception.isCritical());
        assertTrue(exception.getRecoverySuggestion().contains("1, 2, 3, 5, 10"));
        assertTrue(exception.getRecoverySuggestion().contains("/reload"));
    }

    @Test
    @DisplayName("missingApiKey factory creates critical exception")
    void testMissingApiKeyFactory() {
        ConfigException exception = ConfigException.missingApiKey("groq");

        assertTrue(exception.getMessage().contains("API key not configured"));
        assertTrue(exception.getMessage().contains("groq"));
        assertEquals(MineWrightException.ErrorCode.CONFIG_MISSING_KEY, exception.getErrorCode());
        assertTrue(exception.isCritical(), "Missing API key should be critical");
        assertTrue(exception.getRecoverySuggestion().contains("GROQ"));
        assertTrue(exception.getRecoverySuggestion().contains("config/minewright-common.toml"));
        assertTrue(exception.getRecoverySuggestion().contains("apiKey="));
    }

    @Test
    @DisplayName("missingApiKey factory for different providers")
    void testMissingApiKeyFactoryProviders() {
        ConfigException openai = ConfigException.missingApiKey("openai");
        ConfigException gemini = ConfigException.missingApiKey("gemini");

        assertTrue(openai.getRecoverySuggestion().contains("OPENAI"));
        assertTrue(gemini.getRecoverySuggestion().contains("GEMINI"));
        assertTrue(openai.getRecoverySuggestion().contains("apiKey=\""));
        assertTrue(gemini.getRecoverySuggestion().contains("apiKey=\""));
    }

    @Test
    @DisplayName("invalidApiKeyFormat factory creates critical exception")
    void testInvalidApiKeyFormatFactory() {
        ConfigException exception = ConfigException.invalidApiKeyFormat("openai", "too short");

        assertTrue(exception.getMessage().contains("Invalid API key format"));
        assertTrue(exception.getMessage().contains("openai"));
        assertTrue(exception.getMessage().contains("too short"));
        assertEquals("***", exception.getConfigValue(), "Should mask the key");
        assertTrue(exception.isCritical(), "Invalid API key format should be critical");
        assertTrue(exception.getRecoverySuggestion().contains("malformed"));
        assertTrue(exception.getRecoverySuggestion().contains("spaces"));
        assertTrue(exception.getRecoverySuggestion().contains("quotes"));
    }

    @Test
    @DisplayName("validationFailed factory creates non-critical exception")
    void testValidationFailedFactory() {
        ConfigException exception = ConfigException.validationFailed("port", "Port out of range: 99999");

        assertTrue(exception.getMessage().contains("Configuration validation failed"));
        assertTrue(exception.getMessage().contains("port"));
        assertTrue(exception.getMessage().contains("Port out of range"));
        assertEquals("port", exception.getConfigKey());
        assertEquals(MineWrightException.ErrorCode.CONFIG_VALIDATION_FAILED, exception.getErrorCode());
        assertFalse(exception.isCritical(), "Validation failures should be non-critical");
        assertTrue(exception.getRecoverySuggestion().contains("/reload"));
    }

    // ========== getDetailedMessage Tests ==========

    @Test
    @DisplayName("getDetailedMessage includes key when present")
    void testGetDetailedMessageWithKey() {
        ConfigException exception = new ConfigException(
            "Error",
            "test.key",
            "value",
            MineWrightException.ErrorCode.CONFIG_INVALID_VALUE,
            "Fix it"
        );

        String detailed = exception.getDetailedMessage();
        assertTrue(detailed.contains("Error"));
        assertTrue(detailed.contains("[key: test.key]"));
    }

    @Test
    @DisplayName("getDetailedMessage includes recovery when present")
    void testGetDetailedMessageWithRecovery() {
        ConfigException exception = new ConfigException(
            "Error",
            null,
            null,
            MineWrightException.ErrorCode.CONFIG_INVALID_VALUE,
            "Recovery suggestion"
        );

        String detailed = exception.getDetailedMessage();
        assertTrue(detailed.contains("Error"));
        assertTrue(detailed.contains("\nRecovery:"));
        assertTrue(detailed.contains("Recovery suggestion"));
    }

    @Test
    @DisplayName("getDetailedMessage includes both key and recovery when present")
    void testGetDetailedMessageWithKeyAndRecovery() {
        ConfigException exception = new ConfigException(
            "Error",
            "full.key",
            null,
            MineWrightException.ErrorCode.CONFIG_INVALID_VALUE,
            "Full recovery"
        );

        String detailed = exception.getDetailedMessage();
        assertTrue(detailed.contains("Error"));
        assertTrue(detailed.contains("[key: full.key]"));
        assertTrue(detailed.contains("\nRecovery:"));
        assertTrue(detailed.contains("Full recovery"));
    }

    @Test
    @DisplayName("getDetailedMessage handles null key and null recovery")
    void testGetDetailedMessageWithNulls() {
        ConfigException exception = new ConfigException(
            "Simple error",
            null,
            null,
            MineWrightException.ErrorCode.CONFIG_INVALID_VALUE,
            null
        );

        String detailed = exception.getDetailedMessage();
        assertEquals("Simple error", detailed);
    }

    // ========== toString Tests ==========

    @Test
    @DisplayName("toString includes all relevant fields")
    void testToStringFormat() {
        ConfigException exception = new ConfigException(
            "Test error",
            "test.key",
            "testValue",
            MineWrightException.ErrorCode.CONFIG_INVALID_VALUE,
            "Fix it",
            true
        );

        String toString = exception.toString();
        assertTrue(toString.contains("ConfigException"));
        assertTrue(toString.contains("key='test.key'"));
        assertTrue(toString.contains("value='testValue'"));
        assertTrue(toString.contains("critical=true"));
        assertTrue(toString.contains("code=" + MineWrightException.ErrorCode.CONFIG_INVALID_VALUE.getCode()));
        assertTrue(toString.contains("message='Test error'"));
    }

    @Test
    @DisplayName("toString shows 'null' for null value")
    void testToStringNullValue() {
        ConfigException exception = new ConfigException(
            "Error",
            "key",
            null,
            MineWrightException.ErrorCode.CONFIG_MISSING_KEY,
            "Add key"
        );

        assertTrue(exception.toString().contains("value='null'"));
    }

    @Test
    @DisplayName("toString shows critical flag correctly")
    void testToStringCriticalFlag() {
        ConfigException critical = new ConfigException(
            "Critical",
            "key",
            "value",
            MineWrightException.ErrorCode.CONFIG_MISSING_KEY,
            "Fix",
            true
        );
        ConfigException nonCritical = new ConfigException(
            "Non-critical",
            "key",
            "value",
            MineWrightException.ErrorCode.CONFIG_INVALID_VALUE,
            "Fix",
            false
        );

        assertTrue(critical.toString().contains("critical=true"));
        assertTrue(nonCritical.toString().contains("critical=false"));
    }

    // ========== Critical Flag Tests ==========

    @Test
    @DisplayName("Critical flag affects error severity")
    void testCriticalFlagSeverity() {
        ConfigException critical = ConfigException.missingRequired("critical.key");
        ConfigException nonCritical = ConfigException.invalidEnum("level", "INVALID", "INFO, WARN");

        assertTrue(critical.isCritical());
        assertFalse(nonCritical.isCritical());
    }

    @Test
    @DisplayName("Critical exceptions have appropriate recovery suggestions")
    void testCriticalRecoverySuggestions() {
        ConfigException critical = ConfigException.missingApiKey("openai");
        ConfigException nonCritical = ConfigException.invalidEnum("level", "DEBUG", "INFO");

        // Critical errors emphasize required action
        assertTrue(critical.getRecoverySuggestion().contains("cannot function"));
        assertTrue(critical.getRecoverySuggestion().contains("API key"));

        // Non-critical errors are more lenient
        assertTrue(nonCritical.getRecoverySuggestion().contains("Valid values are"));
    }

    // ========== Config Key Tests ==========

    @Test
    @DisplayName("getConfigKey returns correct keys")
    void testGetConfigKey() {
        assertEquals("openai.apiKey", ConfigException.missingApiKey("openai").getConfigKey());
        assertEquals("log.level", ConfigException.invalidEnum("log.level", "INVALID", "INFO").getConfigKey());
        assertEquals("retry.maxRetries", ConfigException.missingKey("maxRetries", "retry", true).getConfigKey());
    }

    @Test
    @DisplayName("Config keys include section prefix")
    void testConfigKeySectionPrefix() {
        ConfigException exception = ConfigException.invalidValue("timeout", "0", "1,5,10", "connection");

        assertTrue(exception.getConfigKey().contains("connection."));
        assertTrue(exception.getConfigKey().contains("timeout"));
    }

    // ========== Edge Case Tests ==========

    @Test
    @DisplayName("Exception can be thrown and caught correctly")
    void testExceptionThrowCatch() {
        ConfigException thrown = assertThrows(ConfigException.class, () -> {
            throw ConfigException.missingRequired("test.key");
        });

        assertEquals("Missing required configuration: test.key", thrown.getMessage());
    }

    @Test
    @DisplayName("Exception with cause preserves stack trace")
    void testExceptionCauseStackTrace() {
        Throwable cause = new IllegalArgumentException("Invalid TOML format");
        ConfigException exception = new ConfigException("Parse error", cause);

        assertSame(cause, exception.getCause());
        assertEquals("Invalid TOML format", exception.getCause().getMessage());
    }

    @Test
    @DisplayName("Multiple config exceptions can be chained")
    void testExceptionChaining() {
        Throwable rootCause = new IllegalStateException("Root error");
        ConfigException middle = new ConfigException(
            "Middle config error",
            rootCause
        );
        ConfigException top = new ConfigException(
            "Top config error",
            middle
        );

        assertSame(middle, top.getCause());
        assertSame(rootCause, top.getCause().getCause());
    }

    @Test
    @DisplayName("Factory methods handle empty and null parameters")
    void testFactoryMethodEmptyNullParams() {
        ConfigException emptyKey = ConfigException.missingRequired("");
        ConfigException nullSection = ConfigException.missingKey("key", null, true);
        ConfigException emptyValue = ConfigException.invalidValue("key", "", "1,2,3", "test");

        assertEquals("", emptyKey.getConfigKey());
        assertTrue(nullSection.getMessage().contains("key"));
        assertEquals("", emptyValue.getConfigValue());
    }

    @Test
    @DisplayName("Exception handles very long config keys")
    void testLongConfigKey() {
        StringBuilder longKey = new StringBuilder("very.long.config.key.path.");
        for (int i = 0; i < 50; i++) {
            longKey.append("segment").append(i).append(".");
        }

        ConfigException exception = ConfigException.missingRequired(longKey.toString());
        assertTrue(exception.getConfigKey().length() > 300);
    }

    @Test
    @DisplayName("Exception handles special characters in config values")
    void testSpecialCharactersInValue() {
        String specialValue = "value with \n newlines \t tabs and \"quotes\"";
        ConfigException exception = new ConfigException(
            "Error",
            "key",
            specialValue,
            MineWrightException.ErrorCode.CONFIG_INVALID_VALUE,
            null
        );

        assertEquals(specialValue, exception.getConfigValue());
    }

    @Test
    @DisplayName("Exception is serializable")
    void testExceptionSerialization() {
        ConfigException exception = new ConfigException(
            "Serializable config error",
            "serialize.key",
            "value",
            MineWrightException.ErrorCode.CONFIG_VALIDATION_FAILED,
            "Reload config",
            false
        );

        // Verify exception structure
        assertNotNull(exception);
        assertEquals("Serializable config error", exception.getMessage());
    }

    // ========== Integration Tests ==========

    @Test
    @DisplayName("Exception works in realistic config scenario")
    void testRealisticScenario() {
        // Simulate missing API key scenario
        ConfigException exception = ConfigException.missingApiKey("openai");

        assertTrue(exception.isCritical());
        assertTrue(exception.getRecoverySuggestion().contains("OPENAI"));
        assertTrue(exception.getRecoverySuggestion().contains("config/minewright-common.toml"));

        // In real code, would trigger appropriate error handling
        if (exception.isCritical()) {
            // Would show error to user and disable features
            assertTrue(exception.getRecoverySuggestion().length() > 0);
        }
    }

    @Test
    @DisplayName("Different config errors can be distinguished")
    void testConfigErrorDiscrimination() {
        ConfigException missing = ConfigException.missingRequired("key");
        ConfigException invalid = ConfigException.invalidEnum("level", "BAD", "INFO");
        ConfigException validation = ConfigException.validationFailed("port", "Out of range");

        // Can distinguish by error code
        assertEquals(MineWrightException.ErrorCode.CONFIG_MISSING_KEY, missing.getErrorCode());
        assertEquals(MineWrightException.ErrorCode.CONFIG_INVALID_VALUE, invalid.getErrorCode());
        assertEquals(MineWrightException.ErrorCode.CONFIG_VALIDATION_FAILED, validation.getErrorCode());

        // Can distinguish by critical flag
        assertTrue(missing.isCritical());
        assertFalse(invalid.isCritical());
        assertFalse(validation.isCritical());
    }

    @Test
    @DisplayName("Recovery suggestions are contextually appropriate")
    void testRecoverySuggestions() {
        ConfigException missingKey = ConfigException.missingRequired("apiKey");
        ConfigException invalidValue = ConfigException.invalidEnum("level", "BAD", "INFO,WARN");
        ConfigException invalidUrl = ConfigException.invalidUrl("endpoint", "bad://url");
        ConfigException validationError = ConfigException.validationFailed("port", "negative");

        assertTrue(missingKey.getRecoverySuggestion().contains("config/minewright-common.toml"));
        assertTrue(invalidValue.getRecoverySuggestion().contains("Valid values"));
        assertTrue(invalidUrl.getRecoverySuggestion().contains("valid URL"));
        assertTrue(validationError.getRecoverySuggestion().contains("/reload"));
    }

    @Test
    @DisplayName("Exception maintains error code type safety")
    void testErrorCodeTypeSafety() {
        ConfigException exception = ConfigException.missingRequired("test");

        assertEquals(MineWrightException.ErrorCode.CONFIG_MISSING_KEY, exception.getErrorCode());

        // Can switch on error codes
        switch (exception.getErrorCode()) {
            case CONFIG_MISSING_KEY:
                // Correct branch
                break;
            case CONFIG_INVALID_VALUE:
            case CONFIG_VALIDATION_FAILED:
                fail("Should have entered CONFIG_MISSING_KEY case");
                break;
            default:
                fail("Unexpected error code");
        }
    }

    @Test
    @DisplayName("getDetailedMessage is useful for logging")
    void testGetDetailedMessageForLogging() {
        ConfigException exception = ConfigException.invalidValue(
            "timeout",
            "0",
            "1,5,10",
            "connection"
        );

        String detailed = exception.getDetailedMessage();

        // Should contain all relevant information
        assertTrue(detailed.contains("Invalid value"));
        assertTrue(detailed.contains("[key:"));
        assertTrue(detailed.contains("Recovery:"));
        assertTrue(detailed.contains("1,5,10"));

        // Suitable for logging
        assertNotNull(detailed);
        assertFalse(detailed.isEmpty());
    }
}
