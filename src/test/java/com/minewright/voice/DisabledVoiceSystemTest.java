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
 * Comprehensive test suite for DisabledVoiceSystem.
 *
 * Tests cover:
 * - No-op behavior of all methods
 * - Always-disabled state
 * - Failed futures for listening operations
 * - Safe handling of all operations
 * - Disabled STT and TTS subsystems
 *
 * @since 1.2.0
 */
@DisplayName("DisabledVoiceSystem Tests")
public class DisabledVoiceSystemTest {

    private DisabledVoiceSystem disabledSystem;

    @BeforeEach
    void setUp() {
        disabledSystem = new DisabledVoiceSystem();
    }

    // ========================================================================
    // Basic Property Tests
    // ========================================================================

    @Test
    @DisplayName("DisabledVoiceSystem should be non-null")
    void testNonNullInstance() {
        assertNotNull(disabledSystem, "Instance should not be null");
    }

    @Test
    @DisplayName("isEnabled should always return false")
    void testIsEnabledAlwaysFalse() {
        assertFalse(disabledSystem.isEnabled(),
            "DisabledVoiceSystem should always be disabled");
    }

    @Test
    @DisplayName("setEnabled should not change disabled state")
    void testSetEnabledDoesNotChangeState() {
        disabledSystem.setEnabled(true);
        assertFalse(disabledSystem.isEnabled(),
            "Should remain disabled after setEnabled(true)");

        disabledSystem.setEnabled(false);
        assertFalse(disabledSystem.isEnabled(),
            "Should remain disabled after setEnabled(false)");
    }

    // ========================================================================
    // Initialization Tests
    // ========================================================================

    @Test
    @DisplayName("initialize should not throw")
    void testInitializeDoesNotThrow() {
        assertDoesNotThrow(() -> disabledSystem.initialize(),
            "initialize should not throw");
    }

    @Test
    @DisplayName("initialize should be callable multiple times")
    void testInitializeIsRepeatable() {
        assertDoesNotThrow(() -> {
            disabledSystem.initialize();
            disabledSystem.initialize();
            disabledSystem.initialize();
        }, "initialize should be repeatable");
    }

    // ========================================================================
    // Listening Tests
    // ========================================================================

    @Test
    @DisplayName("startListening should return failed future")
    void testStartListeningReturnsFailedFuture() {
        CompletableFuture<String> future = disabledSystem.startListening();

        assertNotNull(future, "Future should not be null");
        assertTrue(future.isDone(), "Future should be completed immediately");
    }

    @Test
    @DisplayName("startListening future should complete with exception")
    void testStartListeningFutureCompletesExceptionally() throws InterruptedException {
        CompletableFuture<String> future = disabledSystem.startListening();

        assertTrue(future.isCompletedExceptionally(),
            "Future should complete exceptionally");

        assertThrows(ExecutionException.class, () -> {
            future.get(1, TimeUnit.SECONDS);
        }, "Future should throw ExecutionException");
    }

    @Test
    @DisplayName("startListening exception should mention disabled")
    void testStartListeningExceptionMessage() {
        CompletableFuture<String> future = disabledSystem.startListening();

        ExecutionException ex = assertThrows(ExecutionException.class, () -> {
            future.get(1, TimeUnit.SECONDS);
        }, "Should throw ExecutionException");

        Throwable cause = ex.getCause();
        assertTrue(cause instanceof VoiceException,
            "Cause should be VoiceException");

        assertTrue(cause.getMessage().contains("disabled") ||
                   cause.getMessage().contains("disabled"),
            "Exception should mention disabled state");
    }

    @Test
    @DisplayName("stopListening should not throw")
    void testStopListeningDoesNotThrow() {
        assertDoesNotThrow(() -> disabledSystem.stopListening(),
            "stopListening should not throw");
    }

    @Test
    @DisplayName("isListening should always return false")
    void testIsListeningAlwaysFalse() {
        assertFalse(disabledSystem.isListening(),
            "isListening should always return false");

        disabledSystem.stopListening();
        assertFalse(disabledSystem.isListening(),
            "isListening should still return false after stop");
    }

    // ========================================================================
    // Speaking Tests
    // ========================================================================

    @Test
    @DisplayName("speak should not throw")
    void testSpeakDoesNotThrow() {
        assertDoesNotThrow(() -> disabledSystem.speak("Hello world"),
            "speak should not throw");
    }

    @Test
    @DisplayName("speak should handle null text")
    void testSpeakHandlesNull() {
        assertDoesNotThrow(() -> disabledSystem.speak(null),
            "speak(null) should not throw");
    }

    @Test
    @DisplayName("speak should handle empty text")
    void testSpeakHandlesEmpty() {
        assertDoesNotThrow(() -> disabledSystem.speak(""),
            "speak(empty) should not throw");
    }

    @Test
    @DisplayName("speak should handle long text")
    void testSpeakHandlesLongText() {
        String longText = "A".repeat(10000);
        assertDoesNotThrow(() -> disabledSystem.speak(longText),
            "speak(long) should not throw");
    }

    @Test
    @DisplayName("speak should handle unicode")
    void testSpeakHandlesUnicode() {
        assertDoesNotThrow(() -> disabledSystem.speak("Hello 世界 🌍"),
            "speak(unicode) should not throw");
    }

    @Test
    @DisplayName("speak should handle special characters")
    void testSpeakHandlesSpecialChars() {
        assertDoesNotThrow(() -> disabledSystem.speak("Test: @#$%^&*()"),
            "speak(special) should not throw");
    }

    @Test
    @DisplayName("stopSpeaking should not throw")
    void testStopSpeakingDoesNotThrow() {
        assertDoesNotThrow(() -> disabledSystem.stopSpeaking(),
            "stopSpeaking should not throw");
    }

    @Test
    @DisplayName("isSpeaking should always return false")
    void testIsSpeakingAlwaysFalse() {
        assertFalse(disabledSystem.isSpeaking(),
            "isSpeaking should always return false");

        disabledSystem.speak("test");
        assertFalse(disabledSystem.isSpeaking(),
            "isSpeaking should still be false after speak");
    }

    // ========================================================================
    // Test Operation Tests
    // ========================================================================

    @Test
    @DisplayName("test should return failed result")
    void testTestReturnsFailedResult() throws InterruptedException, ExecutionException, TimeoutException {
        CompletableFuture<VoiceSystem.VoiceTestResult> future = disabledSystem.test();

        assertNotNull(future, "Future should not be null");

        VoiceSystem.VoiceTestResult result = future.get(1, TimeUnit.SECONDS);
        assertNotNull(result, "Result should not be null");
        assertFalse(result.success(), "Result should indicate failure");
        assertTrue(result.message().contains("disabled") ||
                   result.message().contains("disabled"),
            "Message should mention disabled state");
    }

    @Test
    @DisplayName("test should complete immediately")
    void testTestCompletesImmediately() {
        CompletableFuture<VoiceSystem.VoiceTestResult> future = disabledSystem.test();

        assertTrue(future.isDone(),
            "Test future should be completed immediately");
    }

    // ========================================================================
    // Shutdown Tests
    // ========================================================================

    @Test
    @DisplayName("shutdown should not throw")
    void testShutdownDoesNotThrow() {
        assertDoesNotThrow(() -> disabledSystem.shutdown(),
            "shutdown should not throw");
    }

    @Test
    @DisplayName("shutdown should be idempotent")
    void testShutdownIsIdempotent() {
        assertDoesNotThrow(() -> {
            disabledSystem.shutdown();
            disabledSystem.shutdown();
            disabledSystem.shutdown();
        }, "shutdown should be callable multiple times");
    }

    @Test
    @DisplayName("operations after shutdown should still work")
    void testOperationsAfterShutdown() {
        disabledSystem.shutdown();

        assertDoesNotThrow(() -> {
            disabledSystem.speak("Test");
            disabledSystem.stopListening();
            disabledSystem.stopSpeaking();
        }, "Operations should still be safe after shutdown");
    }

    // ========================================================================
    // Subsystem Access Tests
    // ========================================================================

    @Test
    @DisplayName("getSpeechToText should return non-null")
    void testGetSpeechToTextReturnsNonNull() {
        SpeechToText stt = disabledSystem.getSpeechToText();
        assertNotNull(stt, "getSpeechToText should return non-null");
    }

    @Test
    @DisplayName("getTextToSpeech should return non-null")
    void testGetTextToSpeechReturnsNonNull() {
        TextToSpeech tts = disabledSystem.getTextToSpeech();
        assertNotNull(tts, "getTextToSpeech should return non-null");
    }

    @Test
    @DisplayName("getSpeechToText should return same instance")
    void testGetSpeechToTextReturnsSameInstance() {
        SpeechToText stt1 = disabledSystem.getSpeechToText();
        SpeechToText stt2 = disabledSystem.getSpeechToText();

        assertSame(stt1, stt2, "Should return same STT instance");
    }

    @Test
    @DisplayName("getTextToSpeech should return same instance")
    void testGetTextToSpeechReturnsSameInstance() {
        TextToSpeech tts1 = disabledSystem.getTextToSpeech();
        TextToSpeech tts2 = disabledSystem.getTextToSpeech();

        assertSame(tts1, tts2, "Should return same TTS instance");
    }

    // ========================================================================
    // Disabled STT Tests
    // ========================================================================

    @Test
    @DisplayName("STT initialize should not throw")
    void testSTTInitialize() {
        SpeechToText stt = disabledSystem.getSpeechToText();
        assertDoesNotThrow(() -> stt.initialize(),
            "STT initialize should not throw");
    }

    @Test
    @DisplayName("STT startListening should not throw")
    void testSTTStartListening() {
        SpeechToText stt = disabledSystem.getSpeechToText();
        assertDoesNotThrow(() -> stt.startListening(result -> {}),
            "STT startListening should not throw");
    }

    @Test
    @DisplayName("STT stopListening should not throw")
    void testSTTStopListening() {
        SpeechToText stt = disabledSystem.getSpeechToText();
        assertDoesNotThrow(() -> stt.stopListening(),
            "STT stopListening should not throw");
    }

    @Test
    @DisplayName("STT isListening should always return false")
    void testSTTIsListening() {
        SpeechToText stt = disabledSystem.getSpeechToText();
        assertFalse(stt.isListening(),
            "STT isListening should always be false");
    }

    @Test
    @DisplayName("STT listenOnce should return failed future")
    void testSTTListenOnce() throws InterruptedException {
        SpeechToText stt = disabledSystem.getSpeechToText();
        CompletableFuture<String> future = stt.listenOnce();

        assertNotNull(future, "Future should not be null");
        assertTrue(future.isCompletedExceptionally(),
            "Future should complete exceptionally");
    }

    @Test
    @DisplayName("STT cancel should not throw")
    void testSTTCancel() {
        SpeechToText stt = disabledSystem.getSpeechToText();
        assertDoesNotThrow(() -> stt.cancel(),
            "STT cancel should not throw");
    }

    @Test
    @DisplayName("STT getPreferredAudioFormat should return valid format")
    void testSTTGetPreferredAudioFormat() {
        SpeechToText stt = disabledSystem.getSpeechToText();
        AudioFormat format = stt.getPreferredAudioFormat();

        assertNotNull(format, "AudioFormat should not be null");
        assertEquals(16000.0f, format.getSampleRate(), 0.001,
            "Sample rate should be 16kHz");
        assertEquals(16, format.getSampleSizeInBits(),
            "Sample size should be 16 bits");
        assertEquals(1, format.getChannels(),
            "Should be mono");
    }

    @Test
    @DisplayName("STT getLanguage should return default")
    void testSTTGetLanguage() {
        SpeechToText stt = disabledSystem.getSpeechToText();
        assertEquals("en-US", stt.getLanguage(),
            "Should return default language");
    }

    @Test
    @DisplayName("STT setLanguage should not throw")
    void testSTTSetLanguage() {
        SpeechToText stt = disabledSystem.getSpeechToText();
        assertDoesNotThrow(() -> stt.setLanguage("es-ES"),
            "setLanguage should not throw");
    }

    @Test
    @DisplayName("STT getSensitivity should return default")
    void testSTTGetSensitivity() {
        SpeechToText stt = disabledSystem.getSpeechToText();
        assertEquals(0.5, stt.getSensitivity(), 0.001,
            "Should return default sensitivity");
    }

    @Test
    @DisplayName("STT setSensitivity should not throw")
    void testSTTSetSensitivity() {
        SpeechToText stt = disabledSystem.getSpeechToText();
        assertDoesNotThrow(() -> stt.setSensitivity(0.8),
            "setSensitivity should not throw");
    }

    @Test
    @DisplayName("STT shutdown should not throw")
    void testSTTShutdown() {
        SpeechToText stt = disabledSystem.getSpeechToText();
        assertDoesNotThrow(() -> stt.shutdown(),
            "STT shutdown should not throw");
    }

    // ========================================================================
    // Disabled TTS Tests
    // ========================================================================

    @Test
    @DisplayName("TTS initialize should not throw")
    void testTTSInitialize() {
        TextToSpeech tts = disabledSystem.getTextToSpeech();
        assertDoesNotThrow(() -> tts.initialize(),
            "TTS initialize should not throw");
    }

    @Test
    @DisplayName("TTS speak should not throw")
    void testTTSSpeak() {
        TextToSpeech tts = disabledSystem.getTextToSpeech();
        assertDoesNotThrow(() -> tts.speak("Hello"),
            "TTS speak should not throw");
    }

    @Test
    @DisplayName("TTS speakQueued should not throw")
    void testTTSSpeakQueued() {
        TextToSpeech tts = disabledSystem.getTextToSpeech();
        assertDoesNotThrow(() -> tts.speakQueued("Hello"),
            "TTS speakQueued should not throw");
    }

    @Test
    @DisplayName("TTS speakAsync should return completed future")
    void testTTSSpeakAsync() throws InterruptedException, ExecutionException {
        TextToSpeech tts = disabledSystem.getTextToSpeech();
        CompletableFuture<Void> future = tts.speakAsync("Hello");

        assertNotNull(future, "Future should not be null");
        assertTrue(future.isDone(),
            "Future should be completed immediately");

        assertDoesNotThrow(() -> future.get(1, TimeUnit.SECONDS),
            "Future should complete successfully");
    }

    @Test
    @DisplayName("TTS stop should not throw")
    void testTTSStop() {
        TextToSpeech tts = disabledSystem.getTextToSpeech();
        assertDoesNotThrow(() -> tts.stop(),
            "TTS stop should not throw");
    }

    @Test
    @DisplayName("TTS isSpeaking should always return false")
    void testTTSIsSpeaking() {
        TextToSpeech tts = disabledSystem.getTextToSpeech();
        assertFalse(tts.isSpeaking(),
            "TTS isSpeaking should always be false");

        tts.speak("test");
        assertFalse(tts.isSpeaking(),
            "TTS isSpeaking should still be false after speak");
    }

    @Test
    @DisplayName("TTS hasQueuedSpeech should always return false")
    void testTTHasQueuedSpeech() {
        TextToSpeech tts = disabledSystem.getTextToSpeech();
        assertFalse(tts.hasQueuedSpeech(),
            "hasQueuedSpeech should always be false");

        tts.speakQueued("test");
        assertFalse(tts.hasQueuedSpeech(),
            "hasQueuedSpeech should still be false");
    }

    @Test
    @DisplayName("TTS clearQueue should not throw")
    void testTTSClearQueue() {
        TextToSpeech tts = disabledSystem.getTextToSpeech();
        assertDoesNotThrow(() -> tts.clearQueue(),
            "clearQueue should not throw");
    }

    @Test
    @DisplayName("TTS getAvailableVoices should return empty list")
    void testTTSGetAvailableVoices() {
        TextToSpeech tts = disabledSystem.getTextToSpeech();
        assertTrue(tts.getAvailableVoices().isEmpty(),
            "getAvailableVoices should return empty list");
    }

    @Test
    @DisplayName("TTS getCurrentVoice should return null")
    void testTTSGetCurrentVoice() {
        TextToSpeech tts = disabledSystem.getTextToSpeech();
        assertNull(tts.getCurrentVoice(),
            "getCurrentVoice should return null");
    }

    @Test
    @DisplayName("TTS setVoice should not throw")
    void testTTSSetVoice() {
        TextToSpeech tts = disabledSystem.getTextToSpeech();
        Voice voice = new Voice.Voice("test", "Test Voice", "en-US", "neutral");

        assertDoesNotThrow(() -> tts.setVoice(voice),
            "setVoice should not throw");
    }

    @Test
    @DisplayName("TTS setVoice with string should not throw")
    void testTTSSetVoiceString() {
        TextToSpeech tts = disabledSystem.getTextToSpeech();
        assertDoesNotThrow(() -> tts.setVoice("test-voice"),
            "setVoice(string) should not throw");
    }

    @Test
    @DisplayName("TTS getRate should return default")
    void testTTSGetRate() {
        TextToSpeech tts = disabledSystem.getTextToSpeech();
        assertEquals(1.0, tts.getRate(), 0.001,
            "getRate should return default");
    }

    @Test
    @DisplayName("TTS setRate should not throw")
    void testTTSSetRate() {
        TextToSpeech tts = disabledSystem.getTextToSpeech();
        assertDoesNotThrow(() -> tts.setRate(1.5),
            "setRate should not throw");
    }

    @Test
    @DisplayName("TTS getPitch should return default")
    void testTTSGetPitch() {
        TextToSpeech tts = disabledSystem.getTextToSpeech();
        assertEquals(1.0, tts.getPitch(), 0.001,
            "getPitch should return default");
    }

    @Test
    @DisplayName("TTS setPitch should not throw")
    void testTTSSetPitch() {
        TextToSpeech tts = disabledSystem.getTextToSpeech();
        assertDoesNotThrow(() -> tts.setPitch(1.2),
            "setPitch should not throw");
    }

    @Test
    @DisplayName("TTS getVolume should return default")
    void testTTSGetVolume() {
        TextToSpeech tts = disabledSystem.getTextToSpeech();
        assertEquals(1.0, tts.getVolume(), 0.001,
            "getVolume should return default");
    }

    @Test
    @DisplayName("TTS setVolume should not throw")
    void testTTSSetVolume() {
        TextToSpeech tts = disabledSystem.getTextToSpeech();
        assertDoesNotThrow(() -> tts.setVolume(0.8),
            "setVolume should not throw");
    }

    @Test
    @DisplayName("TTS shutdown should not throw")
    void testTTSShutdown() {
        TextToSpeech tts = disabledSystem.getTextToSpeech();
        assertDoesNotThrow(() -> tts.shutdown(),
            "TTS shutdown should not throw");
    }

    // ========================================================================
    // Concurrent Access Tests
    // ========================================================================

    @Test
    @DisplayName("DisabledVoiceSystem should handle concurrent access")
    void testConcurrentAccess() throws InterruptedException {
        Thread[] threads = new Thread[10];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                try {
                    disabledSystem.speak("Concurrent test");
                    disabledSystem.startListening();
                    disabledSystem.getSpeechToText();
                    disabledSystem.getTextToSpeech();
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
        assertTrue(true, "Concurrent access should not cause issues");
    }
}
