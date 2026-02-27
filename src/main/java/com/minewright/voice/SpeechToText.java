package com.minewright.voice;

import javax.sound.sampled.AudioFormat;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Interface for speech-to-text (STT) functionality.
 *
 * <p>Implementations convert audio input (typically from microphone)
 * into text that can be processed as natural language commands.</p>
 *
 * <p><b>Configuration:</b></p>
 * STT behavior can be configured via {@link VoiceConfig} including:
 * <ul>
 *   <li>Language/locale</li>
 *   <li>Sensitivity threshold</li>
 *   <li>Push-to-talk vs continuous listening</li>
 *   <li>Timeout duration</li>
 * </ul>
 *
 * @since 1.2.0
 */
public interface SpeechToText {

    /**
     * Initializes the speech-to-text system.
     *
     * <p>This method prepares the STT engine, loads language models,
     * and sets up audio capture resources.</p>
     *
     * @throws VoiceException if initialization fails
     */
    void initialize() throws VoiceException;

    /**
     * Starts capturing audio and transcribing it to text.
     *
     * <p>This method should be non-blocking and return immediately.
     * The provided consumer receives transcription results as they
     * become available.</p>
     *
     * <p>For continuous listening mode, results may include partial
     * transcriptions. For push-to-talk mode, results are complete
     * when the key is released.</p>
     *
     * @param resultConsumer callback that receives transcribed text
     * @throws VoiceException if audio capture fails to start
     */
    void startListening(Consumer<String> resultConsumer) throws VoiceException;

    /**
     * Stops capturing audio and finalizes any pending transcription.
     *
     * <p>This method releases audio resources and ensures any
     * in-progress transcription is completed.</p>
     */
    void stopListening();

    /**
     * Checks if the STT system is currently listening.
     *
     * @return true if actively listening for speech
     */
    boolean isListening();

    /**
     * Starts listening and returns a future with the complete transcription.
     *
     * <p>This is a convenience method that combines startListening()
     * and waiting for a complete result.</p>
     *
     * @return CompletableFuture containing the transcribed text
     * @throws VoiceException if listening fails to start
     */
    CompletableFuture<String> listenOnce() throws VoiceException;

    /**
     * Cancels the current listening operation without waiting for results.
     *
     * <p>This method immediately stops audio capture and discards
     * any pending transcription.</p>
     */
    void cancel();

    /**
     * Returns the preferred audio format for this STT implementation.
     *
     * <p>Audio capture systems should use this format to ensure
     * compatibility with the STT engine.</p>
     *
     * @return AudioFormat for optimal compatibility
     */
    AudioFormat getPreferredAudioFormat();

    /**
     * Returns the language/locale code for transcription.
     *
     * @return language code (e.g., "en-US", "en-GB")
     */
    String getLanguage();

    /**
     * Sets the language/locale for transcription.
     *
     * @param language language code (e.g., "en-US", "en-GB")
     */
    void setLanguage(String language);

    /**
     * Returns the current sensitivity threshold for speech detection.
     *
     * @return sensitivity value (0.0 to 1.0, higher = more sensitive)
     */
    double getSensitivity();

    /**
     * Sets the sensitivity threshold for speech detection.
     *
     * <p>Higher values make the system more sensitive to quiet sounds,
     * but may increase false positives from background noise.</p>
     *
     * @param sensitivity sensitivity value (0.0 to 1.0)
     */
    void setSensitivity(double sensitivity);

    /**
     * Shuts down the STT system and releases all resources.
     */
    void shutdown();
}
