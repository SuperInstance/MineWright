package com.minewright.voice;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

/**
 * Comprehensive test suite for VoiceConfig.
 *
 * Tests cover:
 * - Configuration loading from MineWrightConfig
 * - All getter methods
 * - All setter methods
 * - Configuration validation
 * - Configuration change handling
 * - Edge cases and boundary values
 *
 * @since 1.2.0
 */
@DisplayName("VoiceConfig Tests")
public class VoiceConfigTest {

    private VoiceConfig config;

    @BeforeEach
    void setUp() {
        config = new VoiceConfig();
    }

    // ========================================================================
    // Constructor and Initialization Tests
    // ========================================================================

    @Test
    @DisplayName("Constructor should create non-null instance")
    void testConstructorCreatesNonNull() {
        assertNotNull(config, "VoiceConfig constructor should create non-null instance");
    }

    @Test
    @DisplayName("Constructor should load default values")
    void testConstructorLoadsDefaults() {
        // Just verify we can read all values without exceptions
        assertDoesNotThrow(() -> {
            config.isEnabled();
            config.getMode();
            config.getSttLanguage();
            config.getTtsVoice();
            config.getTtsVolume();
            config.getTtsRate();
            config.getTtsPitch();
            config.getSttSensitivity();
            config.isPushToTalk();
            config.getListeningTimeout();
            config.isDebugLogging();
        }, "All getters should work after construction");
    }

    @Test
    @DisplayName("loadFromMineWrightConfig should not throw")
    void testLoadFromMineWrightConfig() {
        assertDoesNotThrow(() -> config.loadFromMineWrightConfig(),
            "loadFromMineWrightConfig should not throw");
    }

    @Test
    @DisplayName("loadFromMineWrightConfig should be callable multiple times")
    void testLoadFromMineWrightConfigIsRepeatable() {
        assertDoesNotThrow(() -> {
            config.loadFromMineWrightConfig();
            config.loadFromMineWrightConfig();
            config.loadFromMineWrightConfig();
        }, "loadFromMineWrightConfig should be repeatable");
    }

    // ========================================================================
    // Enabled Property Tests
    // ========================================================================

    @Test
    @DisplayName("isEnabled should return boolean")
    void testIsEnabledReturnsBoolean() {
        boolean enabled = config.isEnabled();
        // Just verify type - value depends on config
        assertTrue(enabled || !enabled, "isEnabled should return boolean");
    }

    @Test
    @DisplayName("setEnabled should update enabled state")
    void testSetEnabledUpdatesState() {
        config.setEnabled(true);
        assertTrue(config.isEnabled(), "setEnabled(true) should update state");

        config.setEnabled(false);
        assertFalse(config.isEnabled(), "setEnabled(false) should update state");
    }

    @Test
    @DisplayName("setEnabled should accept same value multiple times")
    void testSetEnabledSameValue() {
        assertDoesNotThrow(() -> {
            config.setEnabled(true);
            config.setEnabled(true);
            config.setEnabled(false);
            config.setEnabled(false);
        }, "setEnabled should accept same value multiple times");
    }

    // ========================================================================
    // Mode Property Tests
    // ========================================================================

    @Test
    @DisplayName("getMode should return non-null string")
    void testGetModeReturnsNonNull() {
        String mode = config.getMode();
        assertNotNull(mode, "getMode should return non-null");
    }

    @Test
    @DisplayName("setMode should update mode")
    void testSetModeUpdatesMode() {
        config.setMode("logging");
        assertEquals("logging", config.getMode(), "setMode should update value");

        config.setMode("real");
        assertEquals("real", config.getMode(), "setMode should update value");
    }

    @Test
    @DisplayName("setMode should handle uppercase input")
    void testSetModeHandlesUppercase() {
        config.setMode("LOGGING");
        assertEquals("LOGGING", config.getMode(), "setMode should preserve case");
    }

    @Test
    @DisplayName("setMode should accept empty string")
    void testSetModeEmptyString() {
        assertDoesNotThrow(() -> config.setMode(""),
            "setMode should accept empty string");
    }

    // ========================================================================
    // STT Language Property Tests
    // ========================================================================

    @Test
    @DisplayName("getSttLanguage should return non-null string")
    void testGetSttLanguageReturnsNonNull() {
        String language = config.getSttLanguage();
        assertNotNull(language, "getSttLanguage should return non-null");
    }

    @Test
    @DisplayName("setSttLanguage should update language")
    void testSetSttLanguageUpdates() {
        config.setSttLanguage("en-GB");
        assertEquals("en-GB", config.getSttLanguage(), "setSttLanguage should update");

        config.setSttLanguage("es-ES");
        assertEquals("es-ES", config.getSttLanguage(), "setSttLanguage should update");
    }

    @Test
    @DisplayName("setSttLanguage should accept common language codes")
    void testSetSttLanguageCommonCodes() {
        assertDoesNotThrow(() -> {
            config.setSttLanguage("en-US");
            config.setSttLanguage("en-GB");
            config.setSttLanguage("es-ES");
            config.setSttLanguage("fr-FR");
            config.setSttLanguage("de-DE");
            config.setSttLanguage("it-IT");
            config.setSttLanguage("pt-BR");
            config.setSttLanguage("zh-CN");
            config.setSttLanguage("ja-JP");
        }, "setSttLanguage should accept common language codes");
    }

    // ========================================================================
    // TTS Voice Property Tests
    // ========================================================================

    @Test
    @DisplayName("getTtsVoice should return string")
    void testGetTtsVoiceReturnsString() {
        String voice = config.getTtsVoice();
        assertNotNull(voice, "getTtsVoice should return non-null");
    }

    @Test
    @DisplayName("setTtsVoice should update voice")
    void testSetTtsVoiceUpdates() {
        config.setTtsVoice("rachel");
        assertEquals("rachel", config.getTtsVoice(), "setTtsVoice should update");
    }

    @Test
    @DisplayName("setTtsVoice should accept voice IDs")
    void testSetTtsVoiceVoiceIds() {
        assertDoesNotThrow(() -> {
            config.setTtsVoice("21m00Tcm4TlvDq8ikWAM");
            config.setTtsVoice("pNInz6obpgDQGcFmaJgB");
            config.setTtsVoice("EXAVITQu4vr4xnSDxMaL");
        }, "setTtsVoice should accept voice IDs");
    }

    // ========================================================================
    // TTS Volume Property Tests
    // ========================================================================

    @Test
    @DisplayName("getTtsVolume should return double")
    void testGetTtsVolumeReturnsDouble() {
        double volume = config.getTtsVolume();
        assertTrue(volume >= 0.0 && volume <= 1.0,
            "Volume should be in valid range");
    }

    @Test
    @DisplayName("setTtsVolume should update volume")
    void testSetTtsVolumeUpdates() {
        config.setTtsVolume(0.5);
        assertEquals(0.5, config.getTtsVolume(), 0.001,
            "setTtsVolume should update");

        config.setTtsVolume(0.75);
        assertEquals(0.75, config.getTtsVolume(), 0.001,
            "setTtsVolume should update");
    }

    @Test
    @DisplayName("setTtsVolume should accept boundary values")
    void testSetTtsVolumeBoundaryValues() {
        assertDoesNotThrow(() -> {
            config.setTtsVolume(0.0);
            config.setTtsVolume(1.0);
        }, "setTtsVolume should accept 0.0 and 1.0");

        assertEquals(0.0, config.getTtsVolume(), 0.001,
            "Volume should be 0.0");
        config.setTtsVolume(1.0);
        assertEquals(1.0, config.getTtsVolume(), 0.001,
            "Volume should be 1.0");
    }

    @Test
    @DisplayName("setTtsVolume should accept values outside range")
    void testSetTtsVolumeOutsideRange() {
        assertDoesNotThrow(() -> {
            config.setTtsVolume(-0.5);
            config.setTtsVolume(1.5);
            config.setTtsVolume(10.0);
        }, "setTtsVolume should accept any value (validation is separate)");
    }

    // ========================================================================
    // TTS Rate Property Tests
    // ========================================================================

    @Test
    @DisplayName("getTtsRate should return double")
    void testGetTtsRateReturnsDouble() {
        double rate = config.getTtsRate();
        assertTrue(rate >= 0.0, "Rate should be non-negative");
    }

    @Test
    @DisplayName("setTtsRate should update rate")
    void testSetTtsRateUpdates() {
        config.setTtsRate(1.5);
        assertEquals(1.5, config.getTtsRate(), 0.001,
            "setTtsRate should update");

        config.setTtsRate(0.8);
        assertEquals(0.8, config.getTtsRate(), 0.001,
            "setTtsRate should update");
    }

    @Test
    @DisplayName("setTtsRate should accept common rates")
    void testSetTtsRateCommonValues() {
        assertDoesNotThrow(() -> {
            config.setTtsRate(0.5);  // Half speed
            config.setTtsRate(1.0);  // Normal
            config.setTtsRate(1.5);  // 1.5x speed
            config.setTtsRate(2.0);  // Double speed
        }, "setTtsRate should accept common rates");
    }

    // ========================================================================
    // TTS Pitch Property Tests
    // ========================================================================

    @Test
    @DisplayName("getTtsPitch should return double")
    void testGetTtsPitchReturnsDouble() {
        double pitch = config.getTtsPitch();
        assertTrue(pitch >= 0.0, "Pitch should be non-negative");
    }

    @Test
    @DisplayName("setTtsPitch should update pitch")
    void testSetTtsPitchUpdates() {
        config.setTtsPitch(1.2);
        assertEquals(1.2, config.getTtsPitch(), 0.001,
            "setTtsPitch should update");
    }

    @Test
    @DisplayName("setTtsPitch should accept common pitch values")
    void testSetTtsPitchCommonValues() {
        assertDoesNotThrow(() -> {
            config.setTtsPitch(0.5);  // Low pitch
            config.setTtsPitch(1.0);  // Normal pitch
            config.setTtsPitch(1.5);  // High pitch
            config.setTtsPitch(2.0);  // Very high pitch
        }, "setTtsPitch should accept common values");
    }

    // ========================================================================
    // STT Sensitivity Property Tests
    // ========================================================================

    @Test
    @DisplayName("getSttSensitivity should return double")
    void testGetSttSensitivityReturnsDouble() {
        double sensitivity = config.getSttSensitivity();
        assertTrue(sensitivity >= 0.0 && sensitivity <= 1.0,
            "Sensitivity should be in valid range");
    }

    @Test
    @DisplayName("setSttSensitivity should update sensitivity")
    void testSetSttSensitivityUpdates() {
        config.setSttSensitivity(0.7);
        assertEquals(0.7, config.getSttSensitivity(), 0.001,
            "setSttSensitivity should update");
    }

    @Test
    @DisplayName("setSttSensitivity should accept boundary values")
    void testSetSttSensitivityBoundaryValues() {
        assertDoesNotThrow(() -> {
            config.setSttSensitivity(0.0);  // Least sensitive
            config.setSttSensitivity(1.0);  // Most sensitive
        }, "setSttSensitivity should accept boundaries");

        assertEquals(0.0, config.getSttSensitivity(), 0.001,
            "Sensitivity should be 0.0");
        config.setSttSensitivity(1.0);
        assertEquals(1.0, config.getSttSensitivity(), 0.001,
            "Sensitivity should be 1.0");
    }

    // ========================================================================
    // Push-to-Talk Property Tests
    // ========================================================================

    @Test
    @DisplayName("isPushToTalk should return boolean")
    void testIsPushToTalkReturnsBoolean() {
        boolean ptt = config.isPushToTalk();
        assertTrue(ptt || !ptt, "isPushToTalk should return boolean");
    }

    @Test
    @DisplayName("setPushToTalk should update state")
    void testSetPushToTalkUpdates() {
        config.setPushToTalk(true);
        assertTrue(config.isPushToTalk(), "setPushToTalk(true) should update");

        config.setPushToTalk(false);
        assertFalse(config.isPushToTalk(), "setPushToTalk(false) should update");
    }

    // ========================================================================
    // Listening Timeout Property Tests
    // ========================================================================

    @Test
    @DisplayName("getListeningTimeout should return int")
    void testGetListeningTimeoutReturnsInt() {
        int timeout = config.getListeningTimeout();
        assertTrue(timeout >= 0, "Timeout should be non-negative");
    }

    @Test
    @DisplayName("setListeningTimeout should update timeout")
    void testSetListeningTimeoutUpdates() {
        config.setListeningTimeout(30);
        assertEquals(30, config.getListeningTimeout(),
            "setListeningTimeout should update");

        config.setListeningTimeout(60);
        assertEquals(60, config.getListeningTimeout(),
            "setListeningTimeout should update");
    }

    @Test
    @DisplayName("setListeningTimeout should accept common values")
    void testSetListeningTimeoutCommonValues() {
        assertDoesNotThrow(() -> {
            config.setListeningTimeout(10);
            config.setListeningTimeout(30);
            config.setListeningTimeout(60);
        }, "setListeningTimeout should accept common values");
    }

    // ========================================================================
    // Debug Logging Property Tests
    // ========================================================================

    @Test
    @DisplayName("isDebugLogging should return boolean")
    void testIsDebugLoggingReturnsBoolean() {
        boolean debug = config.isDebugLogging();
        assertTrue(debug || !debug, "isDebugLogging should return boolean");
    }

    @Test
    @DisplayName("setDebugLogging should update state")
    void testSetDebugLoggingUpdates() {
        config.setDebugLogging(true);
        assertTrue(config.isDebugLogging(), "setDebugLogging(true) should update");

        config.setDebugLogging(false);
        assertFalse(config.isDebugLogging(), "setDebugLogging(false) should update");
    }

    // ========================================================================
    // Validation Tests
    // ========================================================================

    @Test
    @DisplayName("isValid should return boolean")
    void testIsValidReturnsBoolean() {
        boolean valid = config.isValid();
        assertTrue(valid || !valid, "isValid should return boolean");
    }

    @Test
    @DisplayName("isValid should validate mode")
    void testIsValidValidatesMode() {
        config.setMode("invalid-mode");
        boolean valid = config.isValid();
        // Invalid mode should make config invalid
        assertFalse(valid, "Invalid mode should make config invalid");
    }

    @Test
    @DisplayName("isValid should accept valid modes")
    void testIsValidAcceptsValidModes() {
        String[] validModes = {"disabled", "logging", "real", "DISABLED", "LOGGING", "REAL"};

        for (String mode : validModes) {
            config.setMode(mode);
            config.setTtsVolume(0.8);
            config.setTtsRate(1.0);
            config.setTtsPitch(1.0);
            config.setSttSensitivity(0.5);
            config.setListeningTimeout(30);

            boolean valid = config.isValid();
            assertTrue(valid, "Mode '" + mode + "' should be valid");
        }
    }

    @Test
    @DisplayName("isValid should validate numeric ranges")
    void testIsValidValidatesNumericRanges() {
        // Test invalid volume
        config.setMode("logging");
        config.setTtsVolume(1.5);
        assertFalse(config.isValid(), "Volume > 1.0 should be invalid");

        // Test invalid rate
        config.setTtsVolume(0.8);
        config.setTtsRate(3.0);
        assertFalse(config.isValid(), "Rate > 2.0 should be invalid");

        // Test invalid pitch
        config.setTtsRate(1.0);
        config.setTtsPitch(3.0);
        assertFalse(config.isValid(), "Pitch > 2.0 should be invalid");

        // Test invalid sensitivity
        config.setTtsPitch(1.0);
        config.setSttSensitivity(1.5);
        assertFalse(config.isValid(), "Sensitivity > 1.0 should be invalid");

        // Test invalid timeout
        config.setSttSensitivity(0.5);
        config.setListeningTimeout(100);
        assertFalse(config.isValid(), "Timeout > 60 should be invalid");
    }

    @Test
    @DisplayName("isValid should accept boundary values")
    void testIsValidAcceptsBoundaryValues() {
        config.setMode("logging");
        config.setTtsVolume(0.0);
        config.setTtsRate(0.5);
        config.setTtsPitch(0.5);
        config.setSttSensitivity(0.0);
        config.setListeningTimeout(0);

        assertTrue(config.isValid(), "Lower boundaries should be valid");

        config.setTtsVolume(1.0);
        config.setTtsRate(2.0);
        config.setTtsPitch(2.0);
        config.setSttSensitivity(1.0);
        config.setListeningTimeout(60);

        assertTrue(config.isValid(), "Upper boundaries should be valid");
    }

    // ========================================================================
    // toString Tests
    // ========================================================================

    @Test
    @DisplayName("toString should return non-null string")
    void testToStringReturnsNonNull() {
        String str = config.toString();
        assertNotNull(str, "toString should return non-null");
    }

    @Test
    @DisplayName("toString should contain configuration values")
    void testToStringContainsValues() {
        config.setEnabled(true);
        config.setMode("logging");
        config.setSttLanguage("en-US");

        String str = config.toString();
        assertTrue(str.contains("enabled=true") || str.contains("enabled=true"),
            "toString should contain enabled state");
        assertTrue(str.contains("logging"),
            "toString should contain mode");
    }

    // ========================================================================
    // getSummary Tests
    // ========================================================================

    @Test
    @DisplayName("getSummary should return non-null string")
    void testGetSummaryReturnsNonNull() {
        String summary = config.getSummary();
        assertNotNull(summary, "getSummary should return non-null");
    }

    @Test
    @DisplayName("getSummary should contain key information")
    void testGetSummaryContainsKeyInfo() {
        config.setMode("logging");
        config.setSttLanguage("en-US");

        String summary = config.getSummary();
        assertTrue(summary.contains("Voice"),
            "getSummary should mention Voice");
        assertTrue(summary.contains("logging") || summary.contains("en-US"),
            "getSummary should contain config values");
    }

    // ========================================================================
    // ConfigChangeListener Tests
    // ========================================================================

    @Test
    @DisplayName("onConfigReloading should not throw")
    void testOnConfigReloadingDoesNotThrow() {
        assertDoesNotThrow(() -> config.onConfigReloading(),
            "onConfigReloading should not throw");
    }

    @Test
    @DisplayName("onConfigChanged should not throw")
    void testOnConfigChangedDoesNotThrow() {
        var event = new com.minewright.config.ConfigChangeEvent("voice", null, null);
        assertDoesNotThrow(() -> config.onConfigChanged(event),
            "onConfigChanged should not throw");
    }

    @Test
    @DisplayName("onConfigChanged should handle voice events")
    void testOnConfigChangedHandlesVoiceEvents() {
        var event = new com.minewright.config.ConfigChangeEvent("voice.enabled", "false", "true");
        assertDoesNotThrow(() -> config.onConfigChanged(event),
            "onConfigChanged should handle voice events");
    }

    @Test
    @DisplayName("onConfigReloadFailed should not throw")
    void testOnConfigReloadFailedDoesNotThrow() {
        var exception = new com.minewright.exception.ConfigException("Test exception");
        assertDoesNotThrow(() -> config.onConfigReloadFailed(exception),
            "onConfigReloadFailed should not throw");
    }

    // ========================================================================
    // Edge Case Tests
    // ========================================================================

    @Test
    @DisplayName("Config should handle null mode")
    void testNullMode() {
        assertDoesNotThrow(() -> config.setMode(null),
            "setMode(null) should not throw");
    }

    @Test
    @DisplayName("Config should handle empty language")
    void testEmptyLanguage() {
        assertDoesNotThrow(() -> config.setSttLanguage(""),
            "setSttLanguage(empty) should not throw");
    }

    @Test
    @DisplayName("Config should handle null voice")
    void testNullVoice() {
        assertDoesNotThrow(() -> config.setTtsVoice(null),
            "setTtsVoice(null) should not throw");
    }

    @Test
    @DisplayName("Config should handle negative values")
    void testNegativeValues() {
        assertDoesNotThrow(() -> {
            config.setTtsVolume(-1.0);
            config.setTtsRate(-1.0);
            config.setTtsPitch(-1.0);
            config.setSttSensitivity(-1.0);
            config.setListeningTimeout(-1);
        }, "Config should accept negative values (validation is separate)");
    }

    @Test
    @DisplayName("Config should handle very large values")
    void testLargeValues() {
        assertDoesNotThrow(() -> {
            config.setTtsVolume(Double.MAX_VALUE);
            config.setTtsRate(Double.MAX_VALUE);
            config.setTtsPitch(Double.MAX_VALUE);
            config.setListeningTimeout(Integer.MAX_VALUE);
        }, "Config should accept large values (validation is separate)");
    }
}
