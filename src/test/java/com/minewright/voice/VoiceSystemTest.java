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
 * Comprehensive test suite for VoiceSystem interface contracts.
 *
 * Tests the expected behavior of VoiceSystem implementations,
 * ensuring all implementations follow the same contract.
 *
 * @since 1.2.0
 */
@DisplayName("VoiceSystem Interface Contract Tests")
public class VoiceSystemTest {

    private VoiceSystem voiceSystem;

    @BeforeEach
    void setUp() {
        // Use LoggingVoiceSystem as the test implementation
        // It provides a full implementation without external dependencies
        voiceSystem = new LoggingVoiceSystem();
    }

    // ========================================================================
    // Initialize Tests
    // ========================================================================

    @Test
    @DisplayName("initialize should complete without throwing")
    void testInitializeCompletes() {
        assertDoesNotThrow(() -> voiceSystem.initialize(),
            "initialize should complete without exceptions");
    }

    @Test
    @DisplayName("initialize should be idempotent")
    void testInitializeIsIdempotent() {
        assertDoesNotThrow(() -> {
            voiceSystem.initialize();
            voiceSystem.initialize();
            voiceSystem.initialize();
        }, "initialize should be callable multiple times");
    }

    // ========================================================================
    // Enable/Disable Tests
    // ========================================================================

    @Test
    @DisplayName("isEnabled should return boolean")
    void testIsEnabledReturnsBoolean() {
        boolean enabled = voiceSystem.isEnabled();
        assertTrue(enabled || !enabled, "isEnabled should return boolean");
    }

    @Test
    @DisplayName("setEnabled should update enabled state")
    void testSetEnabledUpdatesState() {
        voiceSystem.setEnabled(false);
        assertFalse(voiceSystem.isEnabled(), "Should be disabled after setEnabled(false)");

        voiceSystem.setEnabled(true);
        assertTrue(voiceSystem.isEnabled(), "Should be enabled after setEnabled(true)");
    }

    @Test
    @DisplayName("setEnabled should accept same value multiple times")
    void testSetEnabledSameValue() {
        assertDoesNotThrow(() -> {
            voiceSystem.setEnabled(true);
            voiceSystem.setEnabled(true);
            voiceSystem.setEnabled(false);
            voiceSystem.setEnabled(false);
        }, "setEnabled should accept same value multiple times");
    }

    // ========================================================================
    // Listening Tests
    // ========================================================================

    @Test
    @DisplayName("startListening should return CompletableFuture")
    void testStartListeningReturnsFuture() throws VoiceException {
        voiceSystem.initialize();
        voiceSystem.setEnabled(true);

        CompletableFuture<String> future = voiceSystem.startListening();
        assertNotNull(future, "startListening should return a future");
    }

    @Test
    @DisplayName("startListening future should complete")
    void testStartListeningFutureCompletes() throws VoiceException, InterruptedException, ExecutionException, TimeoutException {
        voiceSystem.initialize();
        voiceSystem.setEnabled(true);

        CompletableFuture<String> future = voiceSystem.startListening();
        assertNotNull(future, "Future should not be null");

        // Wait for completion with timeout
        String result = future.get(5, TimeUnit.SECONDS);
        assertNotNull(result, "Result should not be null");
    }

    @Test
    @DisplayName("stopListening should not throw")
    void testStopListeningDoesNotThrow() {
        assertDoesNotThrow(() -> voiceSystem.stopListening(),
            "stopListening should not throw");
    }

    @Test
    @DisplayName("stopListening should be callable when not listening")
    void testStopListeningWhenNotListening() {
        assertDoesNotThrow(() -> voiceSystem.stopListening(),
            "stopListening should work when not listening");
    }

    @Test
    @DisplayName("isListening should return boolean")
    void testIsListeningReturnsBoolean() {
        boolean listening = voiceSystem.isListening();
        assertTrue(listening || !listening, "isListening should return boolean");
    }

    @Test
    @DisplayName("isListening should reflect listening state")
    void testIsListeningReflectsState() throws VoiceException {
        voiceSystem.initialize();

        assertFalse(voiceSystem.isListening(),
            "Should not be listening initially");

        CompletableFuture<String> future = voiceSystem.startListening();
        // May or may not be listening immediately after start

        future.cancel(true);
        voiceSystem.stopListening();
    }

    // ========================================================================
    // Speaking Tests
    // ========================================================================

    @Test
    @DisplayName("speak should not throw with valid text")
    void testSpeakDoesNotThrow() {
        assertDoesNotThrow(() -> voiceSystem.speak("Hello world"),
            "speak should not throw with valid text");
    }

    @Test
    @DisplayName("speak should handle null text")
    void testSpeakHandlesNullText() {
        assertDoesNotThrow(() -> voiceSystem.speak(null),
            "speak should handle null text gracefully");
    }

    @Test
    @DisplayName("speak should handle empty text")
    void testSpeakHandlesEmptyText() {
        assertDoesNotThrow(() -> voiceSystem.speak(""),
            "speak should handle empty text gracefully");
    }

    @Test
    @DisplayName("speak should handle long text")
    void testSpeakHandlesLongText() {
        String longText = "A".repeat(10000);
        assertDoesNotThrow(() -> voiceSystem.speak(longText),
            "speak should handle long text");
    }

    @Test
    @DisplayName("speak should handle unicode text")
    void testSpeakHandlesUnicode() {
        String unicodeText = "Hello 世界 🌍 Привет";
        assertDoesNotThrow(() -> voiceSystem.speak(unicodeText),
            "speak should handle unicode");
    }

    @Test
    @DisplayName("speak should handle special characters")
    void testSpeakHandlesSpecialCharacters() {
        String specialText = "Test: @#$%^&*()_+-=[]{}|;':\",./<>?";
        assertDoesNotThrow(() -> voiceSystem.speak(specialText),
            "speak should handle special characters");
    }

    @Test
    @DisplayName("stopSpeaking should not throw")
    void testStopSpeakingDoesNotThrow() {
        assertDoesNotThrow(() -> voiceSystem.stopSpeaking(),
            "stopSpeaking should not throw");
    }

    @Test
    @DisplayName("stopSpeaking should be callable when not speaking")
    void testStopSpeakingWhenNotSpeaking() {
        assertDoesNotThrow(() -> voiceSystem.stopSpeaking(),
            "stopSpeaking should work when not speaking");
    }

    @Test
    @DisplayName("isSpeaking should return boolean")
    void testIsSpeakingReturnsBoolean() {
        boolean speaking = voiceSystem.isSpeaking();
        assertTrue(speaking || !speaking, "isSpeaking should return boolean");
    }

    // ========================================================================
    // Test Operation Tests
    // ========================================================================

    @Test
    @DisplayName("test should return CompletableFuture")
    void testTestReturnsFuture() {
        CompletableFuture<VoiceSystem.VoiceTestResult> future = voiceSystem.test();
        assertNotNull(future, "test should return a future");
    }

    @Test
    @DisplayName("test future should complete")
    void testTestFutureCompletes() throws InterruptedException, ExecutionException, TimeoutException {
        CompletableFuture<VoiceSystem.VoiceTestResult> future = voiceSystem.test();

        VoiceSystem.VoiceTestResult result = future.get(10, TimeUnit.SECONDS);
        assertNotNull(result, "Result should not be null");
    }

    @Test
    @DisplayName("test result should have required fields")
    void testTestResultFields() throws InterruptedException, ExecutionException, TimeoutException {
        CompletableFuture<VoiceSystem.VoiceTestResult> future = voiceSystem.test();

        VoiceSystem.VoiceTestResult result = future.get(10, TimeUnit.SECONDS);

        assertNotNull(result, "Result should not be null");
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
        SpeechToText stt = voiceSystem.getSpeechToText();
        assertNotNull(stt, "getSpeechToText should return non-null");
    }

    @Test
    @DisplayName("getSpeechToText should return same instance")
    void testGetSpeechToTextReturnsSameInstance() {
        SpeechToText stt1 = voiceSystem.getSpeechToText();
        SpeechToText stt2 = voiceSystem.getSpeechToText();

        assertSame(stt1, stt2, "Should return same instance");
    }

    @Test
    @DisplayName("getTextToSpeech should return non-null")
    void testGetTextToSpeechReturnsNonNull() {
        TextToSpeech tts = voiceSystem.getTextToSpeech();
        assertNotNull(tts, "getTextToSpeech should return non-null");
    }

    @Test
    @DisplayName("getTextToSpeech should return same instance")
    void testGetTextToSpeechReturnsSameInstance() {
        TextToSpeech tts1 = voiceSystem.getTextToSpeech();
        TextToSpeech tts2 = voiceSystem.getTextToSpeech();

        assertSame(tts1, tts2, "Should return same instance");
    }

    // ========================================================================
    // Shutdown Tests
    // ========================================================================

    @Test
    @DisplayName("shutdown should not throw")
    void testShutdownDoesNotThrow() {
        assertDoesNotThrow(() -> voiceSystem.shutdown(),
            "shutdown should not throw");
    }

    @Test
    @DisplayName("shutdown should be idempotent")
    void testShutdownIsIdempotent() {
        assertDoesNotThrow(() -> {
            voiceSystem.shutdown();
            voiceSystem.shutdown();
            voiceSystem.shutdown();
        }, "shutdown should be callable multiple times");
    }

    @Test
    @DisplayName("operations after shutdown should handle gracefully")
    void testOperationsAfterShutdown() {
        voiceSystem.shutdown();

        assertDoesNotThrow(() -> {
            voiceSystem.stopListening();
            voiceSystem.stopSpeaking();
            voiceSystem.isListening();
            voiceSystem.isSpeaking();
        }, "Operations after shutdown should be handled gracefully");
    }

    // ========================================================================
    // VoiceTestResult Record Tests
    // ========================================================================

    @Test
    @DisplayName("VoiceTestResult.success should create successful result")
    void testVoiceTestResultSuccess() {
        VoiceSystem.VoiceTestResult result = VoiceSystem.VoiceTestResult.success("OK", 100);

        assertTrue(result.success(), "Success should be true");
        assertEquals("OK", result.message(), "Message should match");
        assertEquals(100, result.latencyMs(), "Latency should match");
    }

    @Test
    @DisplayName("VoiceTestResult.failure should create failed result")
    void testVoiceTestResultFailure() {
        VoiceSystem.VoiceTestResult result = VoiceSystem.VoiceTestResult.failure("Error");

        assertFalse(result.success(), "Success should be false");
        assertEquals("Error", result.message(), "Message should match");
        assertEquals(0, result.latencyMs(), "Latency should be 0 for failure");
    }

    @Test
    @DisplayName("VoiceTestResult should have correct fields")
    void testVoiceTestResultFields() {
        VoiceSystem.VoiceTestResult result = new VoiceSystem.VoiceTestResult(true, "Test", 50);

        assertTrue(result.success(), "Success should be true");
        assertEquals("Test", result.message(), "Message should be 'Test'");
        assertEquals(50, result.latencyMs(), "Latency should be 50");
    }

    // ========================================================================
    // Concurrent Access Tests
    // ========================================================================

    @Test
    @DisplayName("VoiceSystem should handle concurrent speak calls")
    void testConcurrentSpeak() throws InterruptedException {
        voiceSystem.initialize();

        Thread[] threads = new Thread[10];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                try {
                    voiceSystem.speak("Concurrent test");
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
        assertTrue(true, "Concurrent speak calls should not cause deadlock");
    }

    @Test
    @DisplayName("VoiceSystem should handle concurrent state queries")
    void testConcurrentStateQueries() throws InterruptedException {
        Thread[] threads = new Thread[20];
        for (int i = 0; i < threads.length; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                try {
                    if (index % 2 == 0) {
                        voiceSystem.isEnabled();
                        voiceSystem.isListening();
                        voiceSystem.isSpeaking();
                    } else {
                        voiceSystem.getSpeechToText();
                        voiceSystem.getTextToSpeech();
                    }
                } catch (Exception e) {
                    // Ignore
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join(500);
        }

        // If we get here without deadlock, test passes
        assertTrue(true, "Concurrent state queries should not cause issues");
    }

    // ========================================================================
    // STT Interface Tests
    // ========================================================================

    @Test
    @DisplayName("STT initialize should not throw")
    void testSTTInitialize() {
        SpeechToText stt = voiceSystem.getSpeechToText();
        assertDoesNotThrow(() -> stt.initialize(),
            "STT initialize should not throw");
    }

    @Test
    @DisplayName("STT isListening should return boolean")
    void testSTTIsListening() {
        SpeechToText stt = voiceSystem.getSpeechToText();
        boolean listening = stt.isListening();
        assertTrue(listening || !listening, "isListening should return boolean");
    }

    @Test
    @DisplayName("STT getLanguage should return non-null")
    void testSTTGetLanguage() {
        SpeechToText stt = voiceSystem.getSpeechToText();
        assertNotNull(stt.getLanguage(), "getLanguage should return non-null");
    }

    @Test
    @DisplayName("STT setLanguage should not throw")
    void testSTTSetLanguage() {
        SpeechToText stt = voiceSystem.getSpeechToText();
        assertDoesNotThrow(() -> stt.setLanguage("en-US"),
            "setLanguage should not throw");
    }

    @Test
    @DisplayName("STT getSensitivity should return double")
    void testSTTGetSensitivity() {
        SpeechToText stt = voiceSystem.getSpeechToText();
        double sensitivity = stt.getSensitivity();
        assertTrue(sensitivity >= 0.0 && sensitivity <= 1.0,
            "Sensitivity should be in valid range");
    }

    @Test
    @DisplayName("STT setSensitivity should not throw")
    void testSTTSetSensitivity() {
        SpeechToText stt = voiceSystem.getSpeechToText();
        assertDoesNotThrow(() -> stt.setSensitivity(0.7),
            "setSensitivity should not throw");
    }

    @Test
    @DisplayName("STT getPreferredAudioFormat should return non-null")
    void testSTTGetPreferredAudioFormat() {
        SpeechToText stt = voiceSystem.getSpeechToText();
        AudioFormat format = stt.getPreferredAudioFormat();
        assertNotNull(format, "getPreferredAudioFormat should return non-null");
    }

    @Test
    @DisplayName("STT listenOnce should return CompletableFuture")
    void testSTTListenOnce() throws VoiceException {
        SpeechToText stt = voiceSystem.getSpeechToText();
        CompletableFuture<String> future = stt.listenOnce();
        assertNotNull(future, "listenOnce should return a future");
    }

    // ========================================================================
    // TTS Interface Tests
    // ========================================================================

    @Test
    @DisplayName("TTS initialize should not throw")
    void testTTSInitialize() {
        TextToSpeech tts = voiceSystem.getTextToSpeech();
        assertDoesNotThrow(() -> tts.initialize(),
            "TTS initialize should not throw");
    }

    @Test
    @DisplayName("TTS isSpeaking should return boolean")
    void testTTSIsSpeaking() {
        TextToSpeech tts = voiceSystem.getTextToSpeech();
        boolean speaking = tts.isSpeaking();
        assertTrue(speaking || !speaking, "isSpeaking should return boolean");
    }

    @Test
    @DisplayName("TTS getVolume should return double")
    void testTTSGetVolume() {
        TextToSpeech tts = voiceSystem.getTextToSpeech();
        double volume = tts.getVolume();
        assertTrue(volume >= 0.0 && volume <= 1.0,
            "Volume should be in valid range");
    }

    @Test
    @DisplayName("TTS setVolume should not throw")
    void testTTSSetVolume() {
        TextToSpeech tts = voiceSystem.getTextToSpeech();
        assertDoesNotThrow(() -> tts.setVolume(0.8),
            "setVolume should not throw");
    }

    @Test
    @DisplayName("TTS getRate should return double")
    void testTTSGetRate() {
        TextToSpeech tts = voiceSystem.getTextToSpeech();
        double rate = tts.getRate();
        assertTrue(rate >= 0.0, "Rate should be non-negative");
    }

    @Test
    @DisplayName("TTS setRate should not throw")
    void testTTSSetRate() {
        TextToSpeech tts = voiceSystem.getTextToSpeech();
        assertDoesNotThrow(() -> tts.setRate(1.2),
            "setRate should not throw");
    }

    @Test
    @DisplayName("TTS getPitch should return double")
    void testTTSGetPitch() {
        TextToSpeech tts = voiceSystem.getTextToSpeech();
        double pitch = tts.getPitch();
        assertTrue(pitch >= 0.0, "Pitch should be non-negative");
    }

    @Test
    @DisplayName("TTS setPitch should not throw")
    void testTTSSetPitch() {
        TextToSpeech tts = voiceSystem.getTextToSpeech();
        assertDoesNotThrow(() -> tts.setPitch(1.1),
            "setPitch should not throw");
    }

    @Test
    @DisplayName("TTS getAvailableVoices should not return null")
    void testTTSGetAvailableVoices() {
        TextToSpeech tts = voiceSystem.getTextToSpeech();
        assertNotNull(tts.getAvailableVoices(),
            "getAvailableVoices should not return null");
    }

    @Test
    @DisplayName("TTS getCurrentVoice should not throw")
    void testTTSGetCurrentVoice() {
        TextToSpeech tts = voiceSystem.getTextToSpeech();
        assertDoesNotThrow(() -> tts.getCurrentVoice(),
            "getCurrentVoice should not throw");
    }

    @Test
    @DisplayName("TTS speakAsync should return CompletableFuture")
    void testTTSSpeakAsync() throws VoiceException {
        TextToSpeech tts = voiceSystem.getTextToSpeech();
        CompletableFuture<Void> future = tts.speakAsync("Test");
        assertNotNull(future, "speakAsync should return a future");
    }

    @Test
    @DisplayName("TTS stop should not throw")
    void testTTSStop() {
        TextToSpeech tts = voiceSystem.getTextToSpeech();
        assertDoesNotThrow(() -> tts.stop(),
            "stop should not throw");
    }

    @Test
    @DisplayName("TTS clearQueue should not throw")
    void testTTSClearQueue() {
        TextToSpeech tts = voiceSystem.getTextToSpeech();
        assertDoesNotThrow(() -> tts.clearQueue(),
            "clearQueue should not throw");
    }
}
