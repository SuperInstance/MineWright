package com.minewright.voice;

/**
 * Exception thrown when voice system operations fail.
 *
 * <p>This exception indicates errors in speech-to-text or text-to-speech
 * operations, including initialization failures, audio capture problems,
 * and synthesis errors.</p>
 *
 * @since 1.2.0
 */
public class VoiceException extends Exception {
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new voice exception with the specified detail message.
     *
     * @param message the detail message
     */
    public VoiceException(String message) {
        super(message);
    }

    /**
     * Constructs a new voice exception with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public VoiceException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new voice exception with the specified cause.
     *
     * @param cause the cause of the exception
     */
    public VoiceException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates a voice exception for initialization failures.
     */
    public static VoiceException initializationFailed(String reason, Throwable cause) {
        return new VoiceException("Voice system initialization failed: " + reason, cause);
    }

    /**
     * Creates a voice exception for audio capture failures.
     */
    public static VoiceException audioCaptureFailed(String reason, Throwable cause) {
        return new VoiceException("Audio capture failed: " + reason, cause);
    }

    /**
     * Creates a voice exception for transcription failures.
     */
    public static VoiceException transcriptionFailed(String reason, Throwable cause) {
        return new VoiceException("Transcription failed: " + reason, cause);
    }

    /**
     * Creates a voice exception for synthesis failures.
     */
    public static VoiceException synthesisFailed(String reason, Throwable cause) {
        return new VoiceException("Speech synthesis failed: " + reason, cause);
    }

    /**
     * Creates a voice exception for audio playback failures.
     */
    public static VoiceException playbackFailed(String reason, Throwable cause) {
        return new VoiceException("Audio playback failed: " + reason, cause);
    }

    /**
     * Creates a voice exception for configuration errors.
     */
    public static VoiceException configurationError(String reason) {
        return new VoiceException("Configuration error: " + reason);
    }
}
