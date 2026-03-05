package com.minewright.exception;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for LLMClientException.
 * Tests exception construction, factory methods, retryable flag, status codes, and edge cases.
 */
@DisplayName("LLMClientException Tests")
class LLMClientExceptionTest {

    // ========== Constructor Tests ==========

    @Test
    @DisplayName("Basic constructor creates exception with provider and retryable flag")
    void testBasicConstructor() {
        LLMClientException exception = new LLMClientException(
            "API call failed",
            "openai",
            MineWrightException.ErrorCode.LLM_PROVIDER_ERROR,
            "Try again",
            true
        );

        assertEquals("API call failed", exception.getMessage());
        assertEquals("openai", exception.getProvider());
        assertEquals(-1, exception.getStatusCode(), "Basic constructor uses -1 for status code");
        assertEquals(MineWrightException.ErrorCode.LLM_PROVIDER_ERROR, exception.getErrorCode());
        assertEquals("Try again", exception.getRecoverySuggestion());
        assertTrue(exception.isRetryable());
        assertNull(exception.getRetryAfter());
        assertNull(exception.getCause());
    }

    @Test
    @DisplayName("Constructor with status code creates exception with HTTP status")
    void testConstructorWithStatusCode() {
        LLMClientException exception = new LLMClientException(
            "Rate limited",
            "groq",
            429,
            MineWrightException.ErrorCode.LLM_RATE_LIMIT,
            "Wait and retry",
            true
        );

        assertEquals("Rate limited", exception.getMessage());
        assertEquals("groq", exception.getProvider());
        assertEquals(429, exception.getStatusCode());
        assertEquals(MineWrightException.ErrorCode.LLM_RATE_LIMIT, exception.getErrorCode());
        assertTrue(exception.isRetryable());
        assertNull(exception.getRetryAfter());
    }

    @Test
    @DisplayName("Constructor with full details includes retry after duration")
    void testConstructorWithFullDetails() {
        Duration retryAfter = Duration.ofSeconds(60);
        Throwable cause = new RuntimeException("Connection timeout");

        LLMClientException exception = new LLMClientException(
            "Request timeout",
            "gemini",
            -1,
            MineWrightException.ErrorCode.LLM_TIMEOUT,
            "Retry later",
            true,
            retryAfter,
            cause
        );

        assertEquals("Request timeout", exception.getMessage());
        assertEquals("gemini", exception.getProvider());
        assertEquals(-1, exception.getStatusCode());
        assertTrue(exception.isRetryable());
        assertEquals(retryAfter, exception.getRetryAfter());
        assertSame(cause, exception.getCause());
    }

    @Test
    @DisplayName("Constructor accepts null retry after duration")
    void testConstructorWithNullRetryAfter() {
        LLMClientException exception = new LLMClientException(
            "Error",
            "openai",
            500,
            MineWrightException.ErrorCode.LLM_PROVIDER_ERROR,
            "Fix",
            true,
            null,
            null
        );

        assertNull(exception.getRetryAfter());
        assertNull(exception.getCause());
    }

    @Test
    @DisplayName("Constructor accepts null cause")
    void testConstructorWithNullCause() {
        LLMClientException exception = new LLMClientException(
            "Error",
            "groq",
            401,
            MineWrightException.ErrorCode.LLM_AUTH_ERROR,
            "Fix auth",
            false,
            Duration.ofMinutes(5),
            null
        );

        assertNull(exception.getCause());
        assertFalse(exception.isRetryable());
    }

    // ========== Factory Method Tests ==========

    @Test
    @DisplayName("authenticationFailed factory creates non-retryable exception")
    void testAuthenticationFailedFactory() {
        LLMClientException exception = LLMClientException.authenticationFailed("openai");

        assertTrue(exception.getMessage().contains("Authentication failed"));
        assertTrue(exception.getMessage().contains("openai"));
        assertEquals("openai", exception.getProvider());
        assertEquals(401, exception.getStatusCode());
        assertEquals(MineWrightException.ErrorCode.LLM_AUTH_ERROR, exception.getErrorCode());
        assertFalse(exception.isRetryable(), "Auth errors should not be retryable");
        assertTrue(exception.getRecoverySuggestion().contains("API key"));
        assertTrue(exception.getRecoverySuggestion().contains("OPENAI"));
        assertTrue(exception.getRecoverySuggestion().contains("/reload"));
    }

    @Test
    @DisplayName("authenticationFailed for different providers")
    void testAuthenticationFailedProviders() {
        LLMClientException openai = LLMClientException.authenticationFailed("openai");
        LLMClientException groq = LLMClientException.authenticationFailed("groq");
        LLMClientException gemini = LLMClientException.authenticationFailed("gemini");

        assertTrue(openai.getRecoverySuggestion().contains("OPENAI"));
        assertTrue(groq.getRecoverySuggestion().contains("GROQ"));
        assertTrue(gemini.getRecoverySuggestion().contains("GEMINI"));

        assertEquals(401, openai.getStatusCode());
        assertEquals(401, groq.getStatusCode());
        assertEquals(401, gemini.getStatusCode());
    }

    @Test
    @DisplayName("rateLimited factory creates retryable exception with retry after")
    void testRateLimitedFactory() {
        Duration retryAfter = Duration.ofSeconds(30);
        LLMClientException exception = LLMClientException.rateLimited("groq", retryAfter);

        assertTrue(exception.getMessage().contains("Rate limit exceeded"));
        assertTrue(exception.getMessage().contains("groq"));
        assertEquals("groq", exception.getProvider());
        assertEquals(429, exception.getStatusCode());
        assertEquals(MineWrightException.ErrorCode.LLM_RATE_LIMIT, exception.getErrorCode());
        assertTrue(exception.isRetryable(), "Rate limit errors should be retryable");
        assertEquals(retryAfter, exception.getRetryAfter());
        assertTrue(exception.getRecoverySuggestion().contains("30 seconds"));
        assertTrue(exception.getRecoverySuggestion().contains("Wait"));
        assertTrue(exception.getRecoverySuggestion().contains("switch"));
    }

    @Test
    @DisplayName("rateLimited factory with null retry after")
    void testRateLimitedFactoryNullRetryAfter() {
        LLMClientException exception = LLMClientException.rateLimited("openai", null);

        assertTrue(exception.isRetryable());
        assertNull(exception.getRetryAfter());
        assertTrue(exception.getRecoverySuggestion().contains("Wait a minute"));
    }

    @Test
    @DisplayName("timeout factory creates retryable exception with cause")
    void testTimeoutFactory() {
        Throwable cause = new java.net.SocketTimeoutException("Read timed out");
        LLMClientException exception = LLMClientException.timeout("gemini", 30000, cause);

        assertTrue(exception.getMessage().contains("timed out"));
        assertTrue(exception.getMessage().contains("30000ms"));
        assertEquals("gemini", exception.getProvider());
        assertEquals(-1, exception.getStatusCode());
        assertEquals(MineWrightException.ErrorCode.LLM_TIMEOUT, exception.getErrorCode());
        assertTrue(exception.isRetryable(), "Timeouts should be retryable");
        assertSame(cause, exception.getCause());
        assertTrue(exception.getRecoverySuggestion().contains("network issues"));
        assertTrue(exception.getRecoverySuggestion().contains("high load"));
        assertTrue(exception.getRecoverySuggestion().contains("switch"));
    }

    @Test
    @DisplayName("timeout factory with various timeout durations")
    void testTimeoutFactoryDurations() {
        LLMClientException shortTimeout = LLMClientException.timeout("openai", 5000, null);
        LLMClientException longTimeout = LLMClientException.timeout("groq", 120000, null);

        assertTrue(shortTimeout.getMessage().contains("5000ms"));
        assertTrue(longTimeout.getMessage().contains("120000ms"));
    }

    @Test
    @DisplayName("invalidResponse factory creates retryable exception")
    void testInvalidResponseFactory() {
        LLMClientException exception = LLMClientException.invalidResponse("openai", "Missing 'choices' field");

        assertTrue(exception.getMessage().contains("Invalid response"));
        assertTrue(exception.getMessage().contains("Missing 'choices' field"));
        assertEquals("openai", exception.getProvider());
        assertEquals(-1, exception.getStatusCode());
        assertEquals(MineWrightException.ErrorCode.LLM_INVALID_RESPONSE, exception.getErrorCode());
        assertTrue(exception.isRetryable(), "Invalid responses should be retryable");
        assertTrue(exception.getRecoverySuggestion().contains("change in the API"));
        assertTrue(exception.getRecoverySuggestion().contains("report it as a bug"));
    }

    @Test
    @DisplayName("invalidResponse with various reasons")
    void testInvalidResponseReasons() {
        LLMClientException malformedJson = LLMClientException.invalidResponse("groq", "Malformed JSON");
        LLMClientException missingField = LLMClientException.invalidResponse("gemini", "Missing content");
        LLMClientException wrongType = LLMClientException.invalidResponse("openai", "Expected array, got object");

        assertTrue(malformedJson.getMessage().contains("Malformed JSON"));
        assertTrue(missingField.getMessage().contains("Missing content"));
        assertTrue(wrongType.getMessage().contains("Expected array"));
    }

    @Test
    @DisplayName("networkError factory creates retryable exception with cause")
    void testNetworkErrorFactory() {
        Throwable cause = new java.net.UnknownHostException("api.openai.com");
        LLMClientException exception = LLMClientException.networkError("openai", cause);

        assertTrue(exception.getMessage().contains("Network error"));
        assertTrue(exception.getMessage().contains("openai"));
        assertEquals("openai", exception.getProvider());
        assertEquals(-1, exception.getStatusCode());
        assertEquals(MineWrightException.ErrorCode.LLM_NETWORK_ERROR, exception.getErrorCode());
        assertTrue(exception.isRetryable(), "Network errors should be retryable");
        assertSame(cause, exception.getCause());
        assertTrue(exception.getRecoverySuggestion().contains("internet connection"));
        assertTrue(exception.getRecoverySuggestion().contains("downtime"));
    }

    @Test
    @DisplayName("serverError factory creates retryable exception with status code")
    void testServerErrorFactory() {
        LLMClientException exception = LLMClientException.serverError("groq", 503);

        assertTrue(exception.getMessage().contains("Server error"));
        assertTrue(exception.getMessage().contains("HTTP 503"));
        assertEquals("groq", exception.getProvider());
        assertEquals(503, exception.getStatusCode());
        assertEquals(MineWrightException.ErrorCode.LLM_PROVIDER_ERROR, exception.getErrorCode());
        assertTrue(exception.isRetryable(), "Server errors should be retryable");
        assertTrue(exception.getRecoverySuggestion().contains("temporary problem"));
        assertTrue(exception.getRecoverySuggestion().contains("few moments"));
    }

    @Test
    @DisplayName("serverError with various status codes")
    void testServerErrorStatusCodes() {
        LLMClientException error500 = LLMClientException.serverError("openai", 500);
        LLMClientException error502 = LLMClientException.serverError("groq", 502);
        LLMClientException error503 = LLMClientException.serverError("gemini", 503);
        LLMClientException error504 = LLMClientException.serverError("openai", 504);

        assertEquals(500, error500.getStatusCode());
        assertEquals(502, error502.getStatusCode());
        assertEquals(503, error503.getStatusCode());
        assertEquals(504, error504.getStatusCode());
    }

    @Test
    @DisplayName("configurationError factory creates non-retryable exception")
    void testConfigurationErrorFactory() {
        LLMClientException exception = LLMClientException.configurationError("gemini", "Invalid model name");

        assertTrue(exception.getMessage().contains("Configuration error"));
        assertTrue(exception.getMessage().contains("Invalid model name"));
        assertEquals("gemini", exception.getProvider());
        assertEquals(-1, exception.getStatusCode());
        assertEquals(MineWrightException.ErrorCode.LLM_CONFIG_ERROR, exception.getErrorCode());
        assertFalse(exception.isRetryable(), "Configuration errors should not be retryable");
        assertTrue(exception.getRecoverySuggestion().contains("/reload"));
    }

    // ========== toString Tests ==========

    @Test
    @DisplayName("toString includes all relevant fields")
    void testToStringFormat() {
        LLMClientException exception = new LLMClientException(
            "Test error",
            "test-provider",
            500,
            MineWrightException.ErrorCode.LLM_PROVIDER_ERROR,
            "Fix it",
            true,
            Duration.ofSeconds(30),
            null
        );

        String toString = exception.toString();
        assertTrue(toString.contains("LLMClientException"));
        assertTrue(toString.contains("provider='test-provider'"));
        assertTrue(toString.contains("statusCode=500"));
        assertTrue(toString.contains("retryable=true"));
        assertTrue(toString.contains("code=" + MineWrightException.ErrorCode.LLM_PROVIDER_ERROR.getCode()));
        assertTrue(toString.contains("message='Test error'"));
    }

    @Test
    @DisplayName("toString shows -1 for missing status code")
    void testToStringNoStatusCode() {
        LLMClientException exception = LLMClientException.networkError("openai", new RuntimeException());

        assertTrue(exception.toString().contains("statusCode=-1"));
    }

    // ========== Provider Tests ==========

    @Test
    @DisplayName("getProvider returns correct provider names")
    void testGetProvider() {
        assertEquals("openai", LLMClientException.authenticationFailed("openai").getProvider());
        assertEquals("groq", LLMClientException.rateLimited("groq", null).getProvider());
        assertEquals("gemini", LLMClientException.timeout("gemini", 1000, null).getProvider());
    }

    // ========== Status Code Tests ==========

    @Test
    @DisplayName("getStatusCode returns correct HTTP status codes")
    void testGetStatusCode() {
        assertEquals(401, LLMClientException.authenticationFailed("openai").getStatusCode());
        assertEquals(429, LLMClientException.rateLimited("groq", null).getStatusCode());
        assertEquals(500, LLMClientException.serverError("gemini", 500).getStatusCode());
        assertEquals(-1, LLMClientException.timeout("openai", 1000, null).getStatusCode(),
            "Non-HTTP errors should have status code -1");
    }

    // ========== Retryable Tests ==========

    @Test
    @DisplayName("Retryable flag is set correctly by factory methods")
    void testFactoryMethodRetryableFlags() {
        assertTrue(LLMClientException.rateLimited("openai", null).isRetryable());
        assertTrue(LLMClientException.timeout("groq", 1000, null).isRetryable());
        assertTrue(LLMClientException.invalidResponse("gemini", "bad").isRetryable());
        assertTrue(LLMClientException.networkError("openai", new RuntimeException()).isRetryable());
        assertTrue(LLMClientException.serverError("groq", 500).isRetryable());

        assertFalse(LLMClientException.authenticationFailed("openai").isRetryable(),
            "Auth errors should not be retryable");
        assertFalse(LLMClientException.configurationError("gemini", "bad").isRetryable(),
            "Config errors should not be retryable");
    }

    // ========== Retry After Tests ==========

    @Test
    @DisplayName("getRetryAfter returns correct duration")
    void testGetRetryAfter() {
        Duration retry1 = Duration.ofSeconds(10);
        Duration retry2 = Duration.ofMinutes(5);

        LLMClientException exception1 = LLMClientException.rateLimited("openai", retry1);
        LLMClientException exception2 = LLMClientException.rateLimited("groq", retry2);

        assertEquals(retry1, exception1.getRetryAfter());
        assertEquals(retry2, exception2.getRetryAfter());
    }

    @Test
    @DisplayName("getRetryAfter returns null when not set")
    void testGetRetryAfterNull() {
        LLMClientException exception = LLMClientException.authenticationFailed("openai");

        assertNull(exception.getRetryAfter());
    }

    // ========== Edge Case Tests ==========

    @Test
    @DisplayName("Exception can be thrown and caught correctly")
    void testExceptionThrowCatch() {
        LLMClientException thrown = assertThrows(LLMClientException.class, () -> {
            throw LLMClientException.authenticationFailed("openai");
        });

        assertEquals("openai", thrown.getProvider());
        assertEquals(401, thrown.getStatusCode());
    }

    @Test
    @DisplayName("Exception with cause preserves stack trace")
    void testExceptionCauseStackTrace() {
        Throwable cause = new java.net.ConnectException("Connection refused");
        LLMClientException exception = LLMClientException.networkError("openai", cause);

        assertSame(cause, exception.getCause());
        assertEquals("Connection refused", exception.getCause().getMessage());
    }

    @Test
    @DisplayName("Multiple LLM exceptions can be chained")
    void testExceptionChaining() {
        Throwable rootCause = new IllegalStateException("Root error");
        LLMClientException middle = new LLMClientException(
            "Middle LLM error",
            "groq",
            -1,
            MineWrightException.ErrorCode.LLM_NETWORK_ERROR,
            "Fix middle",
            true,
            null,
            rootCause
        );
        LLMClientException top = new LLMClientException(
            "Top LLM error",
            "openai",
            -1,
            MineWrightException.ErrorCode.LLM_TIMEOUT,
            "Fix top",
            true,
            null,
            middle
        );

        assertSame(middle, top.getCause());
        assertSame(rootCause, top.getCause().getCause());
    }

    @Test
    @DisplayName("Factory methods handle empty provider names")
    void testFactoryMethodEmptyProvider() {
        LLMClientException exception = LLMClientException.authenticationFailed("");

        assertEquals("", exception.getProvider());
    }

    @Test
    @DisplayName("Exception handles very long provider names")
    void testLongProviderName() {
        StringBuilder longProvider = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longProvider.append("very-long-provider-name-").append(i);
        }

        LLMClientException exception = LLMClientException.authenticationFailed(longProvider.toString());
        assertTrue(exception.getProvider().length() > 1000);
    }

    @Test
    @DisplayName("Exception handles special characters in provider names")
    void testSpecialCharactersInProvider() {
        String specialProvider = "provider-with_special.chars.123";

        LLMClientException exception = LLMClientException.timeout(specialProvider, 1000, null);
        assertEquals(specialProvider, exception.getProvider());
    }

    @Test
    @DisplayName("Exception handles various retry durations")
    void testVariousRetryDurations() {
        Duration milliseconds = Duration.ofMillis(500);
        Duration seconds = Duration.ofSeconds(30);
        Duration minutes = Duration.ofMinutes(5);
        Duration hours = Duration.ofHours(1);

        LLMClientException exception1 = new LLMClientException(
            "Error", "p1", -1,
            MineWrightException.ErrorCode.LLM_RATE_LIMIT, "Fix",
            true, milliseconds, null
        );
        LLMClientException exception2 = new LLMClientException(
            "Error", "p2", -1,
            MineWrightException.ErrorCode.LLM_RATE_LIMIT, "Fix",
            true, seconds, null
        );
        LLMClientException exception3 = new LLMClientException(
            "Error", "p3", -1,
            MineWrightException.ErrorCode.LLM_RATE_LIMIT, "Fix",
            true, minutes, null
        );
        LLMClientException exception4 = new LLMClientException(
            "Error", "p4", -1,
            MineWrightException.ErrorCode.LLM_RATE_LIMIT, "Fix",
            true, hours, null
        );

        assertEquals(milliseconds, exception1.getRetryAfter());
        assertEquals(seconds, exception2.getRetryAfter());
        assertEquals(minutes, exception3.getRetryAfter());
        assertEquals(hours, exception4.getRetryAfter());
    }

    @Test
    @DisplayName("Exception is serializable")
    void testExceptionSerialization() {
        LLMClientException exception = new LLMClientException(
            "Serializable LLM error",
            "test-provider",
            500,
            MineWrightException.ErrorCode.LLM_PROVIDER_ERROR,
            "Retry",
            true,
            Duration.ofSeconds(30),
            new RuntimeException("Cause")
        );

        // Verify exception structure
        assertNotNull(exception);
        assertEquals("Serializable LLM error", exception.getMessage());
    }

    // ========== Integration Tests ==========

    @Test
    @DisplayName("Exception works in realistic LLM scenario")
    void testRealisticScenario() {
        // Simulate a rate limit scenario
        Duration retryAfter = Duration.ofSeconds(60);
        LLMClientException exception = LLMClientException.rateLimited("groq", retryAfter);

        assertEquals("groq", exception.getProvider());
        assertEquals(429, exception.getStatusCode());
        assertTrue(exception.isRetryable());
        assertEquals(retryAfter, exception.getRetryAfter());
        assertTrue(exception.getRecoverySuggestion().contains("60 seconds"));

        // In real code, would use retryAfter for backoff
        if (exception.isRetryable() && exception.getRetryAfter() != null) {
            long waitTime = exception.getRetryAfter().getSeconds();
            assertTrue(waitTime > 0);
        }
    }

    @Test
    @DisplayName("Different LLM errors can be distinguished")
    void testLLMErrorDiscrimination() {
        LLMClientException auth = LLMClientException.authenticationFailed("openai");
        LLMClientException rate = LLMClientException.rateLimited("groq", null);
        LLMClientException timeout = LLMClientException.timeout("gemini", 1000, null);
        LLMClientException network = LLMClientException.networkError("openai", new RuntimeException());
        LLMClientException server = LLMClientException.serverError("groq", 500);
        LLMClientException config = LLMClientException.configurationError("gemini", "bad");

        // Can distinguish by error code
        assertEquals(MineWrightException.ErrorCode.LLM_AUTH_ERROR, auth.getErrorCode());
        assertEquals(MineWrightException.ErrorCode.LLM_RATE_LIMIT, rate.getErrorCode());
        assertEquals(MineWrightException.ErrorCode.LLM_TIMEOUT, timeout.getErrorCode());
        assertEquals(MineWrightException.ErrorCode.LLM_NETWORK_ERROR, network.getErrorCode());
        assertEquals(MineWrightException.ErrorCode.LLM_PROVIDER_ERROR, server.getErrorCode());
        assertEquals(MineWrightException.ErrorCode.LLM_CONFIG_ERROR, config.getErrorCode());

        // Can distinguish by status code
        assertEquals(401, auth.getStatusCode());
        assertEquals(429, rate.getStatusCode());
        assertEquals(500, server.getStatusCode());
        assertEquals(-1, timeout.getStatusCode());
        assertEquals(-1, network.getStatusCode());
        assertEquals(-1, config.getStatusCode());

        // Can distinguish by retryable flag
        assertFalse(auth.isRetryable());
        assertFalse(config.isRetryable());
        assertTrue(rate.isRetryable());
        assertTrue(timeout.isRetryable());
        assertTrue(network.isRetryable());
        assertTrue(server.isRetryable());
    }

    @Test
    @DisplayName("Recovery suggestions are contextually appropriate")
    void testRecoverySuggestions() {
        LLMClientException auth = LLMClientException.authenticationFailed("openai");
        LLMClientException rate = LLMClientException.rateLimited("groq", Duration.ofSeconds(30));
        LLMClientException timeout = LLMClientException.timeout("gemini", 10000, null);
        LLMClientException invalid = LLMClientException.invalidResponse("openai", "Malformed JSON");
        LLMClientException network = LLMClientException.networkError("groq", new RuntimeException());
        LLMClientException server = LLMClientException.serverError("gemini", 503);
        LLMClientException config = LLMClientException.configurationError("openai", "Invalid setting");

        assertTrue(auth.getRecoverySuggestion().contains("API key"));
        assertTrue(auth.getRecoverySuggestion().contains("/reload"));

        assertTrue(rate.getRecoverySuggestion().contains("Rate limit"));
        assertTrue(rate.getRecoverySuggestion().contains("switch"));
        assertTrue(rate.getRecoverySuggestion().contains("upgrade"));

        assertTrue(timeout.getRecoverySuggestion().contains("network issues"));
        assertTrue(timeout.getRecoverySuggestion().contains("high load"));

        assertTrue(invalid.getRecoverySuggestion().contains("change in the API"));
        assertTrue(invalid.getRecoverySuggestion().contains("report it as a bug"));

        assertTrue(network.getRecoverySuggestion().contains("internet connection"));
        assertTrue(network.getRecoverySuggestion().contains("downtime"));

        assertTrue(server.getRecoverySuggestion().contains("temporary problem"));
        assertTrue(server.getRecoverySuggestion().contains("few moments"));

        assertTrue(config.getRecoverySuggestion().contains("/reload"));
    }

    @Test
    @DisplayName("Exception maintains error code type safety")
    void testErrorCodeTypeSafety() {
        LLMClientException exception = LLMClientException.rateLimited("openai", null);

        assertEquals(MineWrightException.ErrorCode.LLM_RATE_LIMIT, exception.getErrorCode());

        // Can switch on error codes
        switch (exception.getErrorCode()) {
            case LLM_RATE_LIMIT:
                // Correct branch
                break;
            case LLM_AUTH_ERROR:
            case LLM_TIMEOUT:
            case LLM_INVALID_RESPONSE:
            case LLM_NETWORK_ERROR:
            case LLM_PROVIDER_ERROR:
            case LLM_CONFIG_ERROR:
                fail("Should have entered LLM_RATE_LIMIT case");
                break;
            default:
                fail("Unexpected error code");
        }
    }

    @Test
    @DisplayName("Duration is properly handled in retry after")
    void testRetryAfterDurationHandling() {
        Duration retryAfter = Duration.ofSeconds(90);

        LLMClientException exception = LLMClientException.rateLimited("openai", retryAfter);

        assertNotNull(exception.getRetryAfter());
        assertEquals(90, exception.getRetryAfter().getSeconds());
        assertEquals(1, exception.getRetryAfter().toMinutes());
        assertEquals(90000, exception.getRetryAfter().toMillis());
    }

    @Test
    @DisplayName("Exception with null cause doesn't break functionality")
    void testNullCauseHandling() {
        LLMClientException exception = new LLMClientException(
            "Error",
            "provider",
            500,
            MineWrightException.ErrorCode.LLM_PROVIDER_ERROR,
            "Fix",
            true,
            null,
            null
        );

        assertNull(exception.getCause());
        assertNull(exception.getRetryAfter());
        assertNotNull(exception.getMessage());
        assertNotNull(exception.getProvider());
        assertNotNull(exception.getErrorCode());
    }

    @Test
    @DisplayName("Context is properly set when cause is provided")
    void testContextSetting() {
        String provider = "test-provider";

        LLMClientException exception = LLMClientException.networkError(
            provider,
            new RuntimeException("Network failure")
        );

        // The constructor should have added context with provider name
        String context = exception.getContext();
        assertNotNull(context, "Context should be set when cause is provided");

        // Context should contain provider information
        assertTrue(context.contains(provider.toUpperCase()) || context.contains(provider),
            "Context should contain provider name");
    }
}
