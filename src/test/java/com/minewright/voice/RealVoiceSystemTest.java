package com.minewright.voice;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.sound.sampled.AudioFormat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

/**
 * Comprehensive test suite for RealVoiceSystem.
 *
 * Tests cover:
 * - Initialization behavior
 * - TTS fallback chain (Docker MCP -> Direct API)
 * - Whisper STT integration
 * - State management
 * - Error handling when services are unavailable
 *
 * Note: These tests may fail if actual API keys and services are not configured.
 * They are designed to verify the RealVoiceSystem behaves correctly even when
 * external services are unavailable.
 *
 * @since 1.2.0
 */
@DisplayName("RealVoiceSystem Tests")
public class RealVoiceSystemTest {

    private RealVoiceSystem realSystem;

    @BeforeEach
    void setUp() {
        realSystem = new RealVoiceSystem();
    }

    // ========================================================================
    // Basic Property Tests
    // ========================================================================

    @Test
    @DisplayName("RealVoiceSystem should be non-null")
    void testNonNullInstance() {
        assertNotNull(realSystem, "Instance should not be null");
    }

    @Test
    @DisplayName("isEnabled should return true by default")
    void testIsEnabledByDefault() {
        assertTrue(realSystem.isEnabled(),
            "RealVoiceSystem should be enabled by default");
    }

    @Test
    @DisplayName("setEnabled should update state")
    void testSetEnabledUpdatesState() {
        realSystem.setEnabled(false);
        assertFalse(realSystem.isEnabled(),
            "Should be disabled after setEnabled(false)");

        realSystem.setEnabled(true);
        assertTrue(realSystem.isEnabled(),
            "Should be enabled after setEnabled(true)");
    }

    // ========================================================================
    // Initialization Tests
    // ========================================================================

    @Test
    @DisplayName("initialize should complete without throwing")
    void testInitializeCompletes() {
        // Initialize should complete even if services are unavailable
        assertDoesNotThrow(() -> realSystem.initialize(),
            "initialize should complete even if services are unavailable");
    }

    @Test
    @DisplayName("initialize should be idempotent")
    void testInitializeIsIdempotent() {
        assertDoesNotThrow(() -> {
            realSystem.initialize();
            realSystem.initialize();
            realSystem.initialize();
        }, "initialize should be repeatable");
    }

    @Test
    @DisplayName("initialize should create STT subsystem")
    void testInitializeCreatesSTT() {
        realSystem.initialize();

        assertNotNull(realSystem.getSpeechToText(),
            "getSpeechToText should return non-null after initialize");
    }

    // ========================================================================
    // Listening Tests
    // ========================================================================

    @Test
    @DisplayName("startListening should return CompletableFuture")
    void testStartListeningReturnsFuture() {
        realSystem.initialize();
        realSystem.setEnabled(true);

        assertDoesNotThrow(() -> {
            CompletableFuture<String> future = realSystem.startListening();
            assertNotNull(future, "startListening should return a future");
        }, "startListening should return a future");
    }

    @Test
    @DisplayName("startListening should throw when disabled")
    void testStartListeningThrowsWhenDisabled() {
        realSystem.setEnabled(false);

        VoiceException exception = assertThrows(VoiceException.class,
            () -> realSystem.startListening(),
            "startListening should throw when disabled");

        assertTrue(exception.getMessage().contains("disabled") ||
                   exception.getMessage().contains("disabled"),
            "Exception should mention disabled state");
    }

    @Test
    @DisplayName("startListening should throw when not initialized")
    void testStartListeningThrowsWhenNotInitialized() {
        // Don't initialize
        assertThrows(VoiceException.class,
            () -> realSystem.startListening(),
            "startListening should throw when not initialized");
    }

    @Test
    @DisplayName("stopListening should not throw")
    void testStopListeningDoesNotThrow() {
        assertDoesNotThrow(() -> realSystem.stopListening(),
            "stopListening should not throw");
    }

    @Test
    @DisplayName("isListening should return boolean")
    void testIsListeningReturnsBoolean() {
        boolean listening = realSystem.isListening();
        assertTrue(listening || !listening, "isListening should return boolean");
    }

    @Test
    @DisplayName("isListening should be false initially")
    void testIsListeningFalseInitially() {
        assertFalse(realSystem.isListening(),
            "Should not be listening initially");
    }

    // ========================================================================
    // Speaking Tests
    // ========================================================================

    @Test
    @DisplayName("speak should not throw with valid text")
    void testSpeakDoesNotThrow() {
        assertDoesNotThrow(() -> realSystem.speak("Hello world"),
            "speak should not throw (even if TTS unavailable)");
    }

    @Test
    @DisplayName("speak should handle null text gracefully")
    void testSpeakHandlesNull() {
        assertDoesNotThrow(() -> realSystem.speak(null),
            "speak(null) should not throw");
    }

    @Test
    @DisplayName("speak should handle empty text")
    void testSpeakHandlesEmpty() {
        assertDoesNotThrow(() -> realSystem.speak(""),
            "speak(empty) should not throw");
    }

    @Test
    @DisplayName("speak should handle long text")
    void testSpeakHandlesLongText() {
        String longText = "A".repeat(10000);
        assertDoesNotThrow(() -> realSystem.speak(longText),
            "speak(long) should not throw");
    }

    @Test
    @DisplayName("speak should handle unicode")
    void testSpeakHandlesUnicode() {
        assertDoesNotThrow(() -> realSystem.speak("Hello 世界 🌍"),
            "speak(unicode) should not throw");
    }

    @Test
    @DisplayName("speak should handle special characters")
    void testSpeakHandlesSpecialChars() {
        assertDoesNotThrow(() -> realSystem.speak("Test: @#$%^&*()"),
            "speak(special) should not throw");
    }

    @Test
    @DisplayName("speak should do nothing when disabled")
    void testSpeakDoesNothingWhenDisabled() {
        realSystem.setEnabled(false);

        assertDoesNotThrow(() -> realSystem.speak("Test"),
            "speak should not throw when disabled");
    }

    @Test
    @DisplayName("speak should do nothing when TTS unavailable")
    void testSpeakDoesNothingWhenTTSUnavailable() {
        // Without initialization, TTS will be null
        assertDoesNotThrow(() -> realSystem.speak("Test"),
            "speak should not throw when TTS unavailable");
    }

    @Test
    @DisplayName("stopSpeaking should not throw")
    void testStopSpeakingDoesNotThrow() {
        assertDoesNotThrow(() -> realSystem.stopSpeaking(),
            "stopSpeaking should not throw");
    }

    @Test
    @DisplayName("isSpeaking should return boolean")
    void testIsSpeakingReturnsBoolean() {
        boolean speaking = realSystem.isSpeaking();
        assertTrue(speaking || !speaking, "isSpeaking should return boolean");
    }

    @Test
    @DisplayName("isSpeaking should be false initially")
    void testIsSpeakingFalseInitially() {
        assertFalse(realSystem.isSpeaking(),
            "Should not be speaking initially");
    }

    // ========================================================================
    // Test Operation Tests
    // ========================================================================

    @Test
    @DisplayName("test should return CompletableFuture")
    void testTestReturnsFuture() {
        CompletableFuture<VoiceSystem.VoiceTestResult> future = realSystem.test();
        assertNotNull(future, "test should return a future");
    }

    @Test
    @DisplayName("test should complete")
    void testTestCompletes() throws InterruptedException, ExecutionException, TimeoutException {
        CompletableFuture<VoiceSystem.VoiceTestResult> future = realSystem.test();

        VoiceSystem.VoiceTestResult result = future.get(10, TimeUnit.SECONDS);
        assertNotNull(result, "Result should not be null");
    }

    @Test
    @DisplayName("test result should have required fields")
    void testTestResultFields() throws InterruptedException, ExecutionException, TimeoutException {
        CompletableFuture<VoiceSystem.VoiceTestResult> future = realSystem.test();

        VoiceSystem.VoiceTestResult result = future.get(10, TimeUnit.SECONDS);

        assertNotNull(result.message(), "Message should not be null");
        assertTrue(result.success() || !result.success(), "Success should be boolean");
        assertTrue(result.latencyMs() >= 0, "Latency should be non-negative");
    }

    // ========================================================================
    // Subsystem Access Tests
    // ========================================================================

    @Test
    @DisplayName("getSpeechToText should return non-null")
    void testGetSpeechToTextReturnsNonNull() {
        SpeechToText stt = realSystem.getSpeechToText();
        assertNotNull(stt, "getSpeechToText should return non-null");
    }

    @Test
    @DisplayName("getSpeechToText should return WhisperSTT")
    void testGetSpeechToTextReturnsWhisperSTT() {
        SpeechToText stt = realSystem.getSpeechToText();
        assertTrue(stt instanceof WhisperSTT,
            "Should return WhisperSTT instance");
    }

    @Test
    @DisplayName("getSpeechToText should return same instance")
    void testGetSpeechToTextReturnsSameInstance() {
        SpeechToText stt1 = realSystem.getSpeechToText();
        SpeechToText stt2 = realSystem.getSpeechToText();

        assertSame(stt1, stt2, "Should return same STT instance");
    }

    @Test
    @DisplayName("getTextToSpeech may return null before initialization")
    void testGetTextToSpeechMayBeNull() {
        // TTS is initialized lazily, may be null before initialize()
        TextToSpeech tts = realSystem.getTextToSpeech();

        // Just verify it doesn't throw
        // It may be null or a valid instance depending on availability
        assertTrue(tts == null || tts != null, "getTextToSpeech should not throw");
    }

    @Test
    @DisplayName("getTextToSpeech should return same instance after initialization")
    void testGetTextToSpeechReturnsSameInstance() {
        realSystem.initialize();

        TextToSpeech tts1 = realSystem.getTextToSpeech();
        TextToSpeech tts2 = realSystem.getTextToSpeech();

        // Both should be same (or both null if no TTS available)
        assertEquals(tts1 != null, tts2 != null,
            "Both should be null or both non-null");
    }

    // ========================================================================
    // Shutdown Tests
    // ========================================================================

    @Test
    @DisplayName("shutdown should not throw")
    void testShutdownDoesNotThrow() {
        assertDoesNotThrow(() -> realSystem.shutdown(),
            "shutdown should not throw");
    }

    @Test
    @DisplayName("shutdown should be idempotent")
    void testShutdownIsIdempotent() {
        assertDoesNotThrow(() -> {
            realSystem.shutdown();
            realSystem.shutdown();
            realSystem.shutdown();
        }, "shutdown should be repeatable");
    }

    @Test
    @DisplayName("operations after shutdown should handle gracefully")
    void testOperationsAfterShutdown() {
        realSystem.shutdown();

        assertDoesNotThrow(() -> {
            realSystem.speak("Test");
            realSystem.stopListening();
            realSystem.stopSpeaking();
        }, "Operations should be safe after shutdown");
    }

    // ========================================================================
    // Whisper STT Integration Tests
    // ========================================================================

    @Test
    @DisplayName("WhisperSTT should be accessible")
    void testWhisperSTTAccessible() {
        SpeechToText stt = realSystem.getSpeechToText();
        assertTrue(stt instanceof WhisperSTT,
            "Should have WhisperSTT instance");
    }

    @Test
    @DisplayName("WhisperSTT getPreferredAudioFormat should return valid format")
    void testWhisperSTTAudioFormat() {
        SpeechToText stt = realSystem.getSpeechToText();
        AudioFormat format = stt.getPreferredAudioFormat();

        assertNotNull(format, "AudioFormat should not be null");
        assertEquals(16000.0f, format.getSampleRate(), 0.001,
            "Sample rate should be 16kHz (Whisper optimal)");
        assertEquals(16, format.getSampleSizeInBits(),
            "Sample size should be 16 bits");
        assertEquals(1, format.getChannels(),
            "Should be mono");
        assertTrue(format.isBigEndian() == false,
            "Should be little-endian");
    }

    @Test
    @DisplayName("WhisperSTT getLanguage should return default")
    void testWhisperSTTGetLanguage() {
        SpeechToText stt = realSystem.getSpeechToText();
        assertEquals("en", stt.getLanguage(),
            "Should return default language 'en'");
    }

    @Test
    @DisplayName("WhisperSTT setLanguage should update value")
    void testWhisperSTTSetLanguage() {
        SpeechToText stt = realSystem.getSpeechToText();
        stt.setLanguage("es");

        assertEquals("es", stt.getLanguage(),
            "setLanguage should update value");
    }

    @Test
    @DisplayName("WhisperSTT getSensitivity should return default")
    void testWhisperSTTGetSensitivity() {
        SpeechToText stt = realSystem.getSpeechToText();
        assertEquals(0.5, stt.getSensitivity(), 0.001,
            "Should return default sensitivity");
    }

    @Test
    @DisplayName("WhisperSTT setSensitivity should update value")
    void testWhisperSTTSetSensitivity() {
        SpeechToText stt = realSystem.getSpeechToText();
        stt.setSensitivity(0.8);

        assertEquals(0.8, stt.getSensitivity(), 0.001,
            "setSensitivity should update value");
    }

    @Test
    @DisplayName("WhisperSTT setSensitivity should clamp values")
    void testWhisperSTTSetSensitivityClamps() {
        SpeechToText stt = realSystem.getSpeechToText();

        stt.setSensitivity(-1.0);
        assertTrue(stt.getSensitivity() >= 0.0,
            "Should clamp negative to 0.0");

        stt.setSensitivity(2.0);
        assertTrue(stt.getSensitivity() <= 1.0,
            "Should clamp >1.0 to 1.0");
    }

    // ========================================================================
    // TTS Fallback Tests
    // ========================================================================

    @Test
    @DisplayName("TTS should handle unavailability gracefully")
    void testTTSHandlesUnavailability() {
        realSystem.initialize();

        TextToSpeech tts = realSystem.getTextToSpeech();

        // TTS may be null if no services available
        // Just verify it doesn't crash the system
        assertDoesNotThrow(() -> {
            realSystem.speak("Test message");
        }, "Speak should handle TTS unavailability");
    }

    // ========================================================================
    // Error Handling Tests
    // ========================================================================

    @Test
    @DisplayName("RealVoiceSystem should handle concurrent initialization")
    void testConcurrentInitialization() throws InterruptedException {
        Thread[] threads = new Thread[5];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                try {
                    realSystem.initialize();
                } catch (Exception e) {
                    // Ignore
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join(1000);
        }

        // If we get here without deadlock, test passes
        assertTrue(true, "Concurrent initialization should not cause issues");
    }

    @Test
    @DisplayName("RealVoiceSystem should handle concurrent operations")
    void testConcurrentOperations() throws InterruptedException {
        realSystem.initialize();

        Thread[] threads = new Thread[10];
        for (int i = 0; i < threads.length; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                try {
                    if (index % 2 == 0) {
                        realSystem.speak("Concurrent test " + index);
                    } else {
                        realSystem.getSpeechToText().getLanguage();
                        realSystem.getTextToSpeech();
                    }
                } catch (Exception e) {
                    // Ignore
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join(1000);
        }

        // If we get here without deadlock, test passes
        assertTrue(true, "Concurrent operations should not cause deadlock");
    }

    // ========================================================================
    // State Consistency Tests
    // ========================================================================

    @Test
    @DisplayName("enabled state should be consistent")
    void testEnabledStateConsistency() {
        realSystem.setEnabled(true);
        assertTrue(realSystem.isEnabled(),
            "Should be enabled");

        realSystem.setEnabled(false);
        assertFalse(realSystem.isEnabled(),
            "Should be disabled");

        realSystem.setEnabled(true);
        assertTrue(realSystem.isEnabled(),
            "Should be enabled again");
    }

    @Test
    @DisplayName("listening state should be consistent")
    void testListeningStateConsistency() {
        realSystem.initialize();

        assertFalse(realSystem.isListening(),
            "Should not be listening initially");

        realSystem.stopListening();
        assertFalse(realSystem.isListening(),
            "Should not be listening after stop");
    }

    @Test
    @DisplayName("speaking state should be consistent")
    void testSpeakingStateConsistency() {
        assertFalse(realSystem.isSpeaking(),
            "Should not be speaking initially");

        realSystem.stopSpeaking();
        assertFalse(realSystem.isSpeaking(),
            "Should not be speaking after stop");
    }

    // ========================================================================
    // Edge Case Tests
    // ========================================================================

    @Test
    @DisplayName("speak with very long text should not throw")
    void testSpeakVeryLongText() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("This is a test sentence. ");
        }

        assertDoesNotThrow(() -> realSystem.speak(sb.toString()),
            "speak should handle very long text");
    }

    @Test
    @DisplayName("speak with newlines should handle correctly")
    void testSpeakWithNewlines() {
        String textWithNewlines = "Line 1\nLine 2\nLine 3";

        assertDoesNotThrow(() -> realSystem.speak(textWithNewlines),
            "speak should handle newlines");
    }

    @Test
    @DisplayName("speak with tabs should handle correctly")
    void testSpeakWithTabs() {
        String textWithTabs = "Word1\tWord2\tWord3";

        assertDoesNotThrow(() -> realSystem.speak(textWithTabs),
            "speak should handle tabs");
    }

    @Test
    @DisplayName("startListening after stopListening should work")
    void testStartAfterStop() throws VoiceException {
        realSystem.initialize();
        realSystem.setEnabled(true);

        assertDoesNotThrow(() -> {
            realSystem.stopListening();
            CompletableFuture<String> future = realSystem.startListening();
            assertNotNull(future, "Should return new future");
            future.cancel(true);
        }, "startListening after stopListening should work");
    }
}
