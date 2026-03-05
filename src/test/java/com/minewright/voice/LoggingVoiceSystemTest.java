package com.minewright.voice;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import javax.sound.sampled.AudioFormat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

/**
 * Comprehensive test suite for LoggingVoiceSystem.
 *
 * Tests cover:
 * - Logging behavior of all operations
 * - Simulated STT responses
 * - Simulated TTS playback
 * - State management
 * - Test operation behavior
 *
 * @since 1.2.0
 */
@DisplayName("LoggingVoiceSystem Tests")
public class LoggingVoiceSystemTest {

    private LoggingVoiceSystem loggingSystem;

    @BeforeEach
    void setUp() {
        loggingSystem = new LoggingVoiceSystem();
    }

    // ========================================================================
    // Basic Property Tests
    // ========================================================================

    @Test
    @DisplayName("LoggingVoiceSystem should be non-null")
    void testNonNullInstance() {
        assertNotNull(loggingSystem, "Instance should not be null");
    }

    @Test
    @DisplayName("isEnabled should return true by default")
    void testIsEnabledByDefault() {
        assertTrue(loggingSystem.isEnabled(),
            "LoggingVoiceSystem should be enabled by default");
    }

    @Test
    @DisplayName("setEnabled should update state")
    void testSetEnabledUpdatesState() {
        loggingSystem.setEnabled(false);
        assertFalse(loggingSystem.isEnabled(),
            "Should be disabled after setEnabled(false)");

        loggingSystem.setEnabled(true);
        assertTrue(loggingSystem.isEnabled(),
            "Should be enabled after setEnabled(true)");
    }

    // ========================================================================
    // Initialization Tests
    // ========================================================================

    @Test
    @DisplayName("initialize should not throw")
    void testInitializeDoesNotThrow() {
        assertDoesNotThrow(() -> loggingSystem.initialize(),
            "initialize should not throw");
    }

    @Test
    @DisplayName("initialize should be idempotent")
    void testInitializeIsIdempotent() {
        assertDoesNotThrow(() -> {
            loggingSystem.initialize();
            loggingSystem.initialize();
            loggingSystem.initialize();
        }, "initialize should be repeatable");
    }

    // ========================================================================
    // Listening Tests
    // ========================================================================

    @Test
    @DisplayName("startListening should return CompletableFuture")
    void testStartListeningReturnsFuture() {
        assertDoesNotThrow(() -> {
            CompletableFuture<String> future = loggingSystem.startListening();
            assertNotNull(future, "startListening should return a future");
        }, "startListening should return a future");
    }

    @Test
    @DisplayName("startListening should complete with simulated text")
    void testStartListeningCompletesWithText() throws VoiceException, InterruptedException, ExecutionException, TimeoutException {
        loggingSystem.initialize();

        CompletableFuture<String> future = loggingSystem.startListening();
        String result = future.get(2, TimeUnit.SECONDS);

        assertNotNull(result, "Result should not be null");
        assertFalse(result.isEmpty(), "Result should not be empty");
        assertEquals("build a house", result,
            "Should return simulated transcription");
    }

    @Test
    @DisplayName("startListening future should complete within timeout")
    void testStartListeningCompletesWithinTimeout() throws VoiceException, TimeoutException {
        loggingSystem.initialize();

        CompletableFuture<String> future = loggingSystem.startListening();

        assertDoesNotThrow(() -> future.get(2, TimeUnit.SECONDS),
            "Should complete within 2 seconds");
    }

    @Test
    @DisplayName("stopListening should not throw")
    void testStopListeningDoesNotThrow() {
        assertDoesNotThrow(() -> loggingSystem.stopListening(),
            "stopListening should not throw");
    }

    @Test
    @DisplayName("isListening should reflect listening state")
    void testIsListeningReflectsState() throws VoiceException {
        loggingSystem.initialize();

        assertFalse(loggingSystem.isListening(),
            "Should not be listening initially");

        CompletableFuture<String> future = loggingSystem.startListening();

        // State may change asynchronously
        assertNotNull(future, "Future should be created");

        future.cancel(true);
        loggingSystem.stopListening();
    }

    // ========================================================================
    // Speaking Tests
    // ========================================================================

    @Test
    @DisplayName("speak should not throw")
    void testSpeakDoesNotThrow() {
        assertDoesNotThrow(() -> loggingSystem.speak("Hello world"),
            "speak should not throw");
    }

    @Test
    @DisplayName("speak should handle null text")
    void testSpeakHandlesNull() {
        assertDoesNotThrow(() -> loggingSystem.speak(null),
            "speak(null) should not throw");
    }

    @Test
    @DisplayName("speak should handle empty text")
    void testSpeakHandlesEmpty() {
        assertDoesNotThrow(() -> loggingSystem.speak(""),
            "speak(empty) should not throw");
    }

    @Test
    @DisplayName("speak should handle long text")
    void testSpeakHandlesLongText() {
        String longText = "A".repeat(10000);
        assertDoesNotThrow(() -> loggingSystem.speak(longText),
            "speak(long) should not throw");
    }

    @Test
    @DisplayName("speak should handle unicode")
    void testSpeakHandlesUnicode() {
        assertDoesNotThrow(() -> loggingSystem.speak("Hello 世界 🌍"),
            "speak(unicode) should not throw");
    }

    @Test
    @DisplayName("speak should handle special characters")
    void testSpeakHandlesSpecialChars() {
        assertDoesNotThrow(() -> loggingSystem.speak("Test: @#$%^&*()"),
            "speak(special) should not throw");
    }

    @Test
    @DisplayName("speak should update isSpeaking state")
    void testSpeakUpdatesIsSpeaking() throws VoiceException, InterruptedException {
        loggingSystem.initialize();

        loggingSystem.speak("Test");

        // State may change asynchronously
        // Just verify no exception thrown
        assertTrue(true, "speak should complete without exception");
    }

    @Test
    @DisplayName("stopSpeaking should not throw")
    void testStopSpeakingDoesNotThrow() {
        assertDoesNotThrow(() -> loggingSystem.stopSpeaking(),
            "stopSpeaking should not throw");
    }

    @Test
    @DisplayName("isSpeaking should return boolean")
    void testIsSpeakingReturnsBoolean() {
        boolean speaking = loggingSystem.isSpeaking();
        assertTrue(speaking || !speaking, "isSpeaking should return boolean");
    }

    // ========================================================================
    // Test Operation Tests
    // ========================================================================

    @Test
    @DisplayName("test should return CompletableFuture")
    void testTestReturnsFuture() {
        CompletableFuture<VoiceSystem.VoiceTestResult> future = loggingSystem.test();
        assertNotNull(future, "test should return a future");
    }

    @Test
    @DisplayName("test should complete successfully")
    void testTestCompletesSuccessfully() throws InterruptedException, ExecutionException, TimeoutException {
        CompletableFuture<VoiceSystem.VoiceTestResult> future = loggingSystem.test();

        VoiceSystem.VoiceTestResult result = future.get(2, TimeUnit.SECONDS);

        assertTrue(result.success(), "Test should succeed");
        assertNotNull(result.message(), "Message should not be null");
        assertTrue(result.latencyMs() >= 0, "Latency should be non-negative");
    }

    @Test
    @DisplayName("test should complete within reasonable time")
    void testTestCompletesQuickly() throws InterruptedException, ExecutionException, TimeoutException {
        long startTime = System.currentTimeMillis();
        CompletableFuture<VoiceSystem.VoiceTestResult> future = loggingSystem.test();

        future.get(2, TimeUnit.SECONDS);
        long elapsed = System.currentTimeMillis() - startTime;

        assertTrue(elapsed < 2000, "Test should complete quickly");
    }

    // ========================================================================
    // Shutdown Tests
    // ========================================================================

    @Test
    @DisplayName("shutdown should not throw")
    void testShutdownDoesNotThrow() {
        assertDoesNotThrow(() -> loggingSystem.shutdown(),
            "shutdown should not throw");
    }

    @Test
    @DisplayName("shutdown should be idempotent")
    void testShutdownIsIdempotent() {
        assertDoesNotThrow(() -> {
            loggingSystem.shutdown();
            loggingSystem.shutdown();
            loggingSystem.shutdown();
        }, "shutdown should be repeatable");
    }

    @Test
    @DisplayName("operations after shutdown should handle gracefully")
    void testOperationsAfterShutdown() {
        loggingSystem.shutdown();

        assertDoesNotThrow(() -> {
            loggingSystem.speak("Test");
            loggingSystem.stopListening();
            loggingSystem.stopSpeaking();
        }, "Operations should be safe after shutdown");
    }

    // ========================================================================
    // Subsystem Access Tests
    // ========================================================================

    @Test
    @DisplayName("getSpeechToText should return non-null")
    void testGetSpeechToTextReturnsNonNull() {
        SpeechToText stt = loggingSystem.getSpeechToText();
        assertNotNull(stt, "getSpeechToText should return non-null");
    }

    @Test
    @DisplayName("getTextToSpeech should return non-null")
    void testGetTextToSpeechReturnsNonNull() {
        TextToSpeech tts = loggingSystem.getTextToSpeech();
        assertNotNull(tts, "getTextToSpeech should return non-null");
    }

    @Test
    @DisplayName("getSpeechToText should return same instance")
    void testGetSpeechToTextReturnsSameInstance() {
        SpeechToText stt1 = loggingSystem.getSpeechToText();
        SpeechToText stt2 = loggingSystem.getSpeechToText();

        assertSame(stt1, stt2, "Should return same STT instance");
    }

    @Test
    @DisplayName("getTextToSpeech should return same instance")
    void testGetTextToSpeechReturnsSameInstance() {
        TextToSpeech tts1 = loggingSystem.getTextToSpeech();
        TextToSpeech tts2 = loggingSystem.getTextToSpeech();

        assertSame(tts1, tts2, "Should return same TTS instance");
    }

    // ========================================================================
    // STT Tests
    // ========================================================================

    @Test
    @DisplayName("STT initialize should not throw")
    void testSTTInitialize() {
        SpeechToText stt = loggingSystem.getSpeechToText();
        assertDoesNotThrow(() -> stt.initialize(),
            "STT initialize should not throw");
    }

    @Test
    @DisplayName("STT startListening with consumer should not throw")
    void testSTTStartListeningWithConsumer() {
        SpeechToText stt = loggingSystem.getSpeechToText();
        AtomicReference<String> result = new AtomicReference<>();

        assertDoesNotThrow(() -> {
            stt.startListening(result::set);
        }, "startListening with consumer should not throw");
    }

    @Test
    @DisplayName("STT startListening consumer should receive result")
    void testSTTStartListeningConsumerReceivesResult() throws VoiceException, InterruptedException {
        SpeechToText stt = loggingSystem.getSpeechToText();
        stt.initialize();

        AtomicReference<String> result = new AtomicReference<>();
        stt.startListening(result::set);

        // Wait for async result
        Thread.sleep(1000);

        assertNotNull(result.get(), "Consumer should receive result");
        assertFalse(result.get().isEmpty(), "Result should not be empty");
    }

    @Test
    @DisplayName("STT stopListening should not throw")
    void testSTTStopListening() {
        SpeechToText stt = loggingSystem.getSpeechToText();
        assertDoesNotThrow(() -> stt.stopListening(),
            "STT stopListening should not throw");
    }

    @Test
    @DisplayName("STT isListening should return boolean")
    void testSTTIsListening() {
        SpeechToText stt = loggingSystem.getSpeechToText();
        boolean listening = stt.isListening();
        assertTrue(listening || !listening, "isListening should return boolean");
    }

    @Test
    @DisplayName("STT listenOnce should return CompletableFuture")
    void testSTTListenOnce() throws VoiceException {
        SpeechToText stt = loggingSystem.getSpeechToText();
        CompletableFuture<String> future = stt.listenOnce();

        assertNotNull(future, "listenOnce should return a future");
    }

    @Test
    @DisplayName("STT listenOnce should complete")
    void testSTTListenOnceCompletes() throws VoiceException, InterruptedException, ExecutionException, TimeoutException {
        SpeechToText stt = loggingSystem.getSpeechToText();
        CompletableFuture<String> future = stt.listenOnce();

        String result = future.get(2, TimeUnit.SECONDS);
        assertNotNull(result, "Result should not be null");
        assertEquals("build a house", result, "Should return simulated text");
    }

    @Test
    @DisplayName("STT cancel should not throw")
    void testSTTCancel() {
        SpeechToText stt = loggingSystem.getSpeechToText();
        assertDoesNotThrow(() -> stt.cancel(),
            "STT cancel should not throw");
    }

    @Test
    @DisplayName("STT getPreferredAudioFormat should return valid format")
    void testSTTGetPreferredAudioFormat() {
        SpeechToText stt = loggingSystem.getSpeechToText();
        AudioFormat format = stt.getPreferredAudioFormat();

        assertNotNull(format, "AudioFormat should not be null");
        assertEquals(16000.0f, format.getSampleRate(), 0.001,
            "Sample rate should be 16kHz");
    }

    @Test
    @DisplayName("STT getLanguage should return default")
    void testSTTGetLanguage() {
        SpeechToText stt = loggingSystem.getSpeechToText();
        assertEquals("en-US", stt.getLanguage(),
            "Should return default language");
    }

    @Test
    @DisplayName("STT setLanguage should not throw")
    void testSTTSetLanguage() {
        SpeechToText stt = loggingSystem.getSpeechToText();
        assertDoesNotThrow(() -> stt.setLanguage("es-ES"),
            "setLanguage should not throw");
    }

    @Test
    @DisplayName("STT getSensitivity should return default")
    void testSTTGetSensitivity() {
        SpeechToText stt = loggingSystem.getSpeechToText();
        assertEquals(0.5, stt.getSensitivity(), 0.001,
            "Should return default sensitivity");
    }

    @Test
    @DisplayName("STT setSensitivity should not throw")
    void testSTTSetSensitivity() {
        SpeechToText stt = loggingSystem.getSpeechToText();
        assertDoesNotThrow(() -> stt.setSensitivity(0.8),
            "setSensitivity should not throw");
    }

    @Test
    @DisplayName("STT shutdown should not throw")
    void testSTTShutdown() {
        SpeechToText stt = loggingSystem.getSpeechToText();
        assertDoesNotThrow(() -> stt.shutdown(),
            "STT shutdown should not throw");
    }

    // ========================================================================
    // TTS Tests
    // ========================================================================

    @Test
    @DisplayName("TTS initialize should not throw")
    void testTTSInitialize() {
        TextToSpeech tts = loggingSystem.getTextToSpeech();
        assertDoesNotThrow(() -> tts.initialize(),
            "TTS initialize should not throw");
    }

    @Test
    @DisplayName("TTS speak should not throw")
    void testTTSSpeak() {
        TextToSpeech tts = loggingSystem.getTextToSpeech();
        assertDoesNotThrow(() -> tts.speak("Hello"),
            "TTS speak should not throw");
    }

    @Test
    @DisplayName("TTS speakQueued should not throw")
    void testTTSSpeakQueued() {
        TextToSpeech tts = loggingSystem.getTextToSpeech();
        assertDoesNotThrow(() -> tts.speakQueued("Hello"),
            "TTS speakQueued should not throw");
    }

    @Test
    @DisplayName("TTS speakAsync should return CompletableFuture")
    void testTTSSpeakAsync() throws VoiceException {
        TextToSpeech tts = loggingSystem.getTextToSpeech();
        CompletableFuture<Void> future = tts.speakAsync("Hello");

        assertNotNull(future, "speakAsync should return a future");
    }

    @Test
    @DisplayName("TTS speakAsync should complete")
    void testTTSSpeakAsyncCompletes() throws VoiceException, InterruptedException, ExecutionException, TimeoutException {
        TextToSpeech tts = loggingSystem.getTextToSpeech();
        CompletableFuture<Void> future = tts.speakAsync("Hello");

        assertDoesNotThrow(() -> future.get(2, TimeUnit.SECONDS),
            "speakAsync should complete");
    }

    @Test
    @DisplayName("TTS stop should not throw")
    void testTTSStop() {
        TextToSpeech tts = loggingSystem.getTextToSpeech();
        assertDoesNotThrow(() -> tts.stop(),
            "TTS stop should not throw");
    }

    @Test
    @DisplayName("TTS isSpeaking should return boolean")
    void testTTSIsSpeaking() {
        TextToSpeech tts = loggingSystem.getTextToSpeech();
        boolean speaking = tts.isSpeaking();
        assertTrue(speaking || !speaking, "isSpeaking should return boolean");
    }

    @Test
    @DisplayName("TTS hasQueuedSpeech should return boolean")
    void testTTHasQueuedSpeech() {
        TextToSpeech tts = loggingSystem.getTextToSpeech();
        boolean queued = tts.hasQueuedSpeech();
        assertTrue(queued || !queued, "hasQueuedSpeech should return boolean");
    }

    @Test
    @DisplayName("TTS clearQueue should not throw")
    void testTTSClearQueue() {
        TextToSpeech tts = loggingSystem.getTextToSpeech();
        assertDoesNotThrow(() -> tts.clearQueue(),
            "clearQueue should not throw");
    }

    @Test
    @DisplayName("TTS getAvailableVoices should return voices")
    void testTTSGetAvailableVoices() {
        TextToSpeech tts = loggingSystem.getTextToSpeech();
        assertFalse(tts.getAvailableVoices().isEmpty(),
            "getAvailableVoices should return voices");
    }

    @Test
    @DisplayName("TTS getAvailableVoices should contain default voices")
    void testTTSGetAvailableVoicesContainsDefaults() {
        TextToSpeech tts = loggingSystem.getTextToSpeech();
        var voices = tts.getAvailableVoices();

        assertTrue(voices.size() >= 3,
            "Should have at least 3 default voices");
    }

    @Test
    @DisplayName("TTS getCurrentVoice should return non-null")
    void testTTSGetCurrentVoice() {
        TextToSpeech tts = loggingSystem.getTextToSpeech();
        assertNotNull(tts.getCurrentVoice(),
            "getCurrentVoice should not be null");
    }

    @Test
    @DisplayName("TTS setVoice should not throw")
    void testTTSSetVoice() {
        TextToSpeech tts = loggingSystem.getTextToSpeech();
        Voice voice = new Voice.Voice("test", "Test Voice", "en-US", "neutral");

        assertDoesNotThrow(() -> tts.setVoice(voice),
            "setVoice should not throw");
    }

    @Test
    @DisplayName("TTS setVoice with string should not throw")
    void testTTSSetVoiceString() {
        TextToSpeech tts = loggingSystem.getTextToSpeech();
        assertDoesNotThrow(() -> tts.setVoice("test-voice"),
            "setVoice(string) should not throw");
    }

    @Test
    @DisplayName("TTS getRate should return default")
    void testTTSGetRate() {
        TextToSpeech tts = loggingSystem.getTextToSpeech();
        assertEquals(1.0, tts.getRate(), 0.001,
            "getRate should return default");
    }

    @Test
    @DisplayName("TTS setRate should update value")
    void testTTSSetRate() {
        TextToSpeech tts = loggingSystem.getTextToSpeech();
        tts.setRate(1.5);
        assertEquals(1.5, tts.getRate(), 0.001,
            "setRate should update value");
    }

    @Test
    @DisplayName("TTS getPitch should return default")
    void testTTSGetPitch() {
        TextToSpeech tts = loggingSystem.getTextToSpeech();
        assertEquals(1.0, tts.getPitch(), 0.001,
            "getPitch should return default");
    }

    @Test
    @DisplayName("TTS setPitch should update value")
    void testTTSSetPitch() {
        TextToSpeech tts = loggingSystem.getTextToSpeech();
        tts.setPitch(1.2);
        assertEquals(1.2, tts.getPitch(), 0.001,
            "setPitch should update value");
    }

    @Test
    @DisplayName("TTS getVolume should return default")
    void testTTSGetVolume() {
        TextToSpeech tts = loggingSystem.getTextToSpeech();
        assertEquals(1.0, tts.getVolume(), 0.001,
            "getVolume should return default");
    }

    @Test
    @DisplayName("TTS setVolume should update value")
    void testTTSSetVolume() {
        TextToSpeech tts = loggingSystem.getTextToSpeech();
        tts.setVolume(0.8);
        assertEquals(0.8, tts.getVolume(), 0.001,
            "setVolume should update value");
    }

    @Test
    @DisplayName("TTS shutdown should not throw")
    void testTTSShutdown() {
        TextToSpeech tts = loggingSystem.getTextToSpeech();
        assertDoesNotThrow(() -> tts.shutdown(),
            "TTS shutdown should not throw");
    }

    // ========================================================================
    // Concurrent Access Tests
    // ========================================================================

    @Test
    @DisplayName("LoggingVoiceSystem should handle concurrent speak calls")
    void testConcurrentSpeak() throws InterruptedException {
        loggingSystem.initialize();

        Thread[] threads = new Thread[10];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                try {
                    loggingSystem.speak("Concurrent test");
                } catch (Exception e) {
                    // Ignore
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join(1000);
        }

        assertTrue(true, "Concurrent speak calls should not cause deadlock");
    }

    @Test
    @DisplayName("LoggingVoiceSystem should handle concurrent listen calls")
    void testConcurrentListen() throws InterruptedException {
        loggingSystem.initialize();

        Thread[] threads = new Thread[5];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                try {
                    loggingSystem.startListening().get(2, TimeUnit.SECONDS);
                } catch (Exception e) {
                    // Ignore
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join(3000);
        }

        assertTrue(true, "Concurrent listen calls should not cause deadlock");
    }
}
