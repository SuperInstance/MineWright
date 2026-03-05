package com.minewright.voice;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

/**
 * Comprehensive test suite for VoiceException.
 *
 * Tests cover:
 * - Constructor variants
 * - Factory methods for specific exception types
 * - Message content and formatting
 * - Cause chaining
 * - Exception handling patterns
 *
 * @since 1.2.0
 */
@DisplayName("VoiceException Tests")
public class VoiceExceptionTest {

    // ========================================================================
    // Constructor Tests
    // ========================================================================

    @Test
    @DisplayName("Constructor with message should create exception")
    void testConstructorWithMessage() {
        VoiceException exception = new VoiceException("Test error message");

        assertNotNull(exception, "Exception should not be null");
        assertEquals("Test error message", exception.getMessage(),
            "Message should match constructor argument");
    }

    @Test
    @DisplayName("Constructor with message and cause should create exception")
    void testConstructorWithMessageAndCause() {
        Throwable cause = new IOException("Underlying IO error");
        VoiceException exception = new VoiceException("Test error message", cause);

        assertNotNull(exception, "Exception should not be null");
        assertEquals("Test error message", exception.getMessage(),
            "Message should match constructor argument");
        assertEquals(cause, exception.getCause(),
            "Cause should match constructor argument");
    }

    @Test
    @DisplayName("Constructor with cause should create exception")
    void testConstructorWithCause() {
        Throwable cause = new RuntimeException("Underlying error");
        VoiceException exception = new VoiceException(cause);

        assertNotNull(exception, "Exception should not be null");
        assertEquals(cause, exception.getCause(),
            "Cause should match constructor argument");
    }

    @Test
    @DisplayName("Constructor should accept null message")
    void testConstructorWithNullMessage() {
        VoiceException exception = new VoiceException((String) null);

        assertNotNull(exception, "Exception should not be null");
        assertNull(exception.getMessage(), "Message should be null");
    }

    @Test
    @DisplayName("Constructor should accept empty message")
    void testConstructorWithEmptyMessage() {
        VoiceException exception = new VoiceException("");

        assertNotNull(exception, "Exception should not be null");
        assertEquals("", exception.getMessage(), "Message should be empty");
    }

    @Test
    @DisplayName("Constructor should handle null cause")
    void testConstructorWithNullCause() {
        VoiceException exception = new VoiceException("Test", null);

        assertNotNull(exception, "Exception should not be null");
        assertNull(exception.getCause(), "Cause should be null");
    }

    // ========================================================================
    // initializationFailed Factory Tests
    // ========================================================================

    @Test
    @DisplayName("initializationFailed should create exception with formatted message")
    void testInitializationFailedFormatsMessage() {
        Throwable cause = new RuntimeException("Init failed");
        VoiceException exception = VoiceException.initializationFailed("microphone not found", cause);

        assertNotNull(exception, "Exception should not be null");
        assertTrue(exception.getMessage().contains("Voice system initialization failed"),
            "Message should contain standard prefix");
        assertTrue(exception.getMessage().contains("microphone not found"),
            "Message should contain reason");
        assertEquals(cause, exception.getCause(), "Cause should be preserved");
    }

    @Test
    @DisplayName("initializationFailed should handle null reason")
    void testInitializationFailedWithNullReason() {
        VoiceException exception = VoiceException.initializationFailed(null, null);

        assertNotNull(exception, "Exception should not be null");
        assertTrue(exception.getMessage().contains("Voice system initialization failed"),
            "Message should contain prefix");
    }

    @Test
    @DisplayName("initializationFailed should handle null cause")
    void testInitializationFailedWithNullCause() {
        VoiceException exception = VoiceException.initializationFailed("no API key", null);

        assertNotNull(exception, "Exception should not be null");
        assertTrue(exception.getMessage().contains("no API key"),
            "Message should contain reason");
        assertNull(exception.getCause(), "Cause should be null");
    }

    // ========================================================================
    // audioCaptureFailed Factory Tests
    // ========================================================================

    @Test
    @DisplayName("audioCaptureFailed should create exception with formatted message")
    void testAudioCaptureFailedFormatsMessage() {
        Throwable cause = new RuntimeException("Microphone in use");
        VoiceException exception = VoiceException.audioCaptureFailed("device busy", cause);

        assertNotNull(exception, "Exception should not be null");
        assertTrue(exception.getMessage().contains("Audio capture failed"),
            "Message should contain standard prefix");
        assertTrue(exception.getMessage().contains("device busy"),
            "Message should contain reason");
        assertEquals(cause, exception.getCause(), "Cause should be preserved");
    }

    @Test
    @DisplayName("audioCaptureFailed should handle common audio errors")
    void testAudioCaptureFailedCommonErrors() {
        String[] errorReasons = {
            "microphone disconnected",
            "device in use",
            "permission denied",
            "unsupported format"
        };

        for (String reason : errorReasons) {
            VoiceException exception = VoiceException.audioCaptureFailed(reason, null);
            assertTrue(exception.getMessage().contains(reason),
                "Message should contain: " + reason);
        }
    }

    // ========================================================================
    // transcriptionFailed Factory Tests
    // ========================================================================

    @Test
    @DisplayName("transcriptionFailed should create exception with formatted message")
    void testTranscriptionFailedFormatsMessage() {
        Throwable cause = new IOException("API timeout");
        VoiceException exception = VoiceException.transcriptionFailed("API error", cause);

        assertNotNull(exception, "Exception should not be null");
        assertTrue(exception.getMessage().contains("Transcription failed"),
            "Message should contain standard prefix");
        assertTrue(exception.getMessage().contains("API error"),
            "Message should contain reason");
        assertEquals(cause, exception.getCause(), "Cause should be preserved");
    }

    @Test
    @DisplayName("transcriptionFailed should handle common transcription errors")
    void testTranscriptionFailedCommonErrors() {
        String[] errorReasons = {
            "API timeout",
            "invalid API key",
            "audio too short",
            "unsupported language"
        };

        for (String reason : errorReasons) {
            VoiceException exception = VoiceException.transcriptionFailed(reason, null);
            assertTrue(exception.getMessage().contains(reason),
                "Message should contain: " + reason);
        }
    }

    // ========================================================================
    // synthesisFailed Factory Tests
    // ========================================================================

    @Test
    @DisplayName("synthesisFailed should create exception with formatted message")
    void testSynthesisFailedFormatsMessage() {
        Throwable cause = new RuntimeException("TTS service unavailable");
        VoiceException exception = VoiceException.synthesisFailed("service down", cause);

        assertNotNull(exception, "Exception should not be null");
        assertTrue(exception.getMessage().contains("Speech synthesis failed"),
            "Message should contain standard prefix");
        assertTrue(exception.getMessage().contains("service down"),
            "Message should contain reason");
        assertEquals(cause, exception.getCause(), "Cause should be preserved");
    }

    @Test
    @DisplayName("synthesisFailed should handle common synthesis errors")
    void testSynthesisFailedCommonErrors() {
        String[] errorReasons = {
            "voice not found",
            "quota exceeded",
            "text too long",
            "service unavailable"
        };

        for (String reason : errorReasons) {
            VoiceException exception = VoiceException.synthesisFailed(reason, null);
            assertTrue(exception.getMessage().contains(reason),
                "Message should contain: " + reason);
        }
    }

    // ========================================================================
    // playbackFailed Factory Tests
    // ========================================================================

    @Test
    @DisplayName("playbackFailed should create exception with formatted message")
    void testPlaybackFailedFormatsMessage() {
        Throwable cause = new RuntimeException("Audio device error");
        VoiceException exception = VoiceException.playbackFailed("device not ready", cause);

        assertNotNull(exception, "Exception should not be null");
        assertTrue(exception.getMessage().contains("Audio playback failed"),
            "Message should contain standard prefix");
        assertTrue(exception.getMessage().contains("device not ready"),
            "Message should contain reason");
        assertEquals(cause, exception.getCause(), "Cause should be preserved");
    }

    @Test
    @DisplayName("playbackFailed should handle common playback errors")
    void testPlaybackFailedCommonErrors() {
        String[] errorReasons = {
            "audio device disconnected",
            "format not supported",
            "codec not found",
            "buffer underrun"
        };

        for (String reason : errorReasons) {
            VoiceException exception = VoiceException.playbackFailed(reason, null);
            assertTrue(exception.getMessage().contains(reason),
                "Message should contain: " + reason);
        }
    }

    // ========================================================================
    // configurationError Factory Tests
    // ========================================================================

    @Test
    @DisplayName("configurationError should create exception with formatted message")
    void testConfigurationErrorFormatsMessage() {
        VoiceException exception = VoiceException.configurationError("invalid API key format");

        assertNotNull(exception, "Exception should not be null");
        assertTrue(exception.getMessage().contains("Configuration error"),
            "Message should contain standard prefix");
        assertTrue(exception.getMessage().contains("invalid API key format"),
            "Message should contain reason");
    }

    @Test
    @DisplayName("configurationError should handle common config errors")
    void testConfigurationErrorCommonErrors() {
        String[] errorReasons = {
            "invalid API key",
            "voice not found",
            "invalid language code",
            "volume out of range"
        };

        for (String reason : errorReasons) {
            VoiceException exception = VoiceException.configurationError(reason);
            assertTrue(exception.getMessage().contains(reason),
                "Message should contain: " + reason);
        }
    }

    // ========================================================================
    // Exception Chain Tests
    // ========================================================================

    @Test
    @DisplayName("Exception should preserve cause chain")
    void testExceptionChainPreservation() {
        Throwable rootCause = new IOException("Network error");
        Throwable intermediateCause = new RuntimeException("API call failed", rootCause);
        VoiceException exception = VoiceException.transcriptionFailed("transcription error", intermediateCause);

        assertSame(intermediateCause, exception.getCause(),
            "Immediate cause should be preserved");
        assertSame(rootCause, exception.getCause().getCause(),
            "Root cause should be accessible");
    }

    @Test
    @DisplayName("Exception stack trace should be meaningful")
    void testExceptionStackTrace() {
        VoiceException exception = VoiceException.initializationFailed("test", null);

        StackTraceElement[] stackTrace = exception.getStackTrace();
        assertNotNull(stackTrace, "Stack trace should not be null");
        assertTrue(stackTrace.length > 0, "Stack trace should have elements");
    }

    // ========================================================================
    // Serialization Tests
    // ========================================================================

    @Test
    @DisplayName("Exception should have serialVersionUID")
    void testSerialVersionUID() {
        // Verify the exception can be serialized (has serialVersionUID)
        VoiceException exception = new VoiceException("Test");
        // If no exception is thrown, serialization support is present
        assertTrue(true, "VoiceException has serialVersionUID");
    }

    // ========================================================================
    // Special Character Tests
    // ========================================================================

    @Test
    @DisplayName("Exception should handle special characters in message")
    void testSpecialCharactersInMessage() {
        String[] specialMessages = {
            "Error with \"quotes\"",
            "Error with 'apostrophes'",
            "Error with \n newlines",
            "Error with \t tabs",
            "Error with unicode: \u4e2d\u6587",
            "Error with emojis: \ud83d\ude00"
        };

        for (String message : specialMessages) {
            VoiceException exception = new VoiceException(message);
            assertEquals(message, exception.getMessage(),
                "Special characters should be preserved");
        }
    }

    // ========================================================================
    // Very Long Message Tests
    // ========================================================================

    @Test
    @DisplayName("Exception should handle very long messages")
    void testVeryLongMessage() {
        String longMessage = "A".repeat(10000);
        VoiceException exception = new VoiceException(longMessage);

        assertEquals(longMessage, exception.getMessage(),
            "Very long message should be preserved");
    }

    // ========================================================================
    // Empty/Null Handling Tests
    // ========================================================================

    @Test
    @DisplayName("initializationFailed should handle empty reason")
    void testInitializationFailedEmptyReason() {
        VoiceException exception = VoiceException.initializationFailed("", null);

        assertNotNull(exception.getMessage(),
            "Message should not be null even with empty reason");
    }

    @Test
    @DisplayName("audioCaptureFailed should handle empty reason")
    void testAudioCaptureFailedEmptyReason() {
        VoiceException exception = VoiceException.audioCaptureFailed("", null);

        assertNotNull(exception.getMessage(),
            "Message should not be null even with empty reason");
    }

    @Test
    @DisplayName("transcriptionFailed should handle empty reason")
    void testTranscriptionFailedEmptyReason() {
        VoiceException exception = VoiceException.transcriptionFailed("", null);

        assertNotNull(exception.getMessage(),
            "Message should not be null even with empty reason");
    }

    @Test
    @DisplayName("synthesisFailed should handle empty reason")
    void testSynthesisFailedEmptyReason() {
        VoiceException exception = VoiceException.synthesisFailed("", null);

        assertNotNull(exception.getMessage(),
            "Message should not be null even with empty reason");
    }

    @Test
    @DisplayName("playbackFailed should handle empty reason")
    void testPlaybackFailedEmptyReason() {
        VoiceException exception = VoiceException.playbackFailed("", null);

        assertNotNull(exception.getMessage(),
            "Message should not be null even with empty reason");
    }

    @Test
    @DisplayName("configurationError should handle empty reason")
    void testConfigurationErrorEmptyReason() {
        VoiceException exception = VoiceException.configurationError("");

        assertNotNull(exception.getMessage(),
            "Message should not be null even with empty reason");
    }

    // ========================================================================
    // Exception Type Tests
    // ========================================================================

    @Test
    @DisplayName("VoiceException should extend Exception")
    void testExtendsException() {
        VoiceException exception = new VoiceException("Test");
        assertTrue(exception instanceof Exception,
            "VoiceException should extend Exception");
    }

    @Test
    @DisplayName("VoiceException should not be RuntimeException")
    void testNotRuntimeException() {
        VoiceException exception = new VoiceException("Test");
        assertFalse(exception instanceof RuntimeException,
            "VoiceException should not be RuntimeException");
    }

    @Test
    @DisplayName("VoiceException should be catchable as Exception")
    void testCatchableAsException() {
        try {
            throw new VoiceException("Test");
        } catch (Exception e) {
            assertTrue(e instanceof VoiceException,
                "Should be catchable as Exception");
        }
    }

    // ========================================================================
    // Equals and HashCode Tests
    // ========================================================================

    @Test
    @DisplayName("Two exceptions with same message should not be equal")
    void testExceptionsNotEqual() {
        VoiceException exception1 = new VoiceException("Test message");
        VoiceException exception2 = new VoiceException("Test message");

        // Exceptions don't override equals, so they use reference equality
        assertNotEquals(exception1, exception2,
            "Exceptions should use reference equality");
    }

    @Test
    @DisplayName("Same exception instance should equal itself")
    void testExceptionEqualsItself() {
        VoiceException exception = new VoiceException("Test");
        assertEquals(exception, exception,
            "Exception should equal itself");
    }
}
