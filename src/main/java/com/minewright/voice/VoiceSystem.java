package com.minewright.voice;

import java.util.concurrent.CompletableFuture;

/**
 * Interface for voice input/output systems.
 *
 * <p>Implementations provide speech-to-text (STT) and text-to-speech (TTS)
 * functionality for the MineWright mod. Voice systems can be enabled/disabled
 * via configuration.</p>
 *
 * <p><b>Usage:</b></p>
 * <pre>{@code
 * VoiceSystem voice = VoiceSystem.getInstance();
 * if (voice.isEnabled()) {
 *     // Start listening for voice input
 *     CompletableFuture<String> input = voice.startListening();
 *     input.thenAccept(text -> processCommand(text));
 *
 *     // Speak response
 *     voice.speak("Task completed!");
 * }
 * }</pre>
 *
 * @since 1.2.0
 */
public interface VoiceSystem {

    /**
     * Initializes the voice system and prepares it for use.
     *
     * <p>This method should be called once during mod initialization.
     * It may load resources, establish connections to voice APIs,
     * or perform other setup tasks.</p>
     *
     * @throws VoiceException if initialization fails
     */
    void initialize() throws VoiceException;

    /**
     * Checks if voice functionality is currently enabled.
     *
     * <p>Even if the voice system is initialized, it may be disabled
     * via configuration or user preference.</p>
     *
     * @return true if voice is enabled and ready to use
     */
    boolean isEnabled();

    /**
     * Enables or disables voice functionality.
     *
     * @param enabled true to enable voice, false to disable
     */
    void setEnabled(boolean enabled);

    /**
     * Starts listening for voice input and returns a future
     * that completes when speech is detected.
     *
     * <p>This method should be non-blocking and return immediately.
     * The returned future completes when the user finishes speaking
     * or a timeout occurs.</p>
     *
     * <p>If push-to-talk mode is enabled, listening starts when
     * the key is held and stops when released.</p>
     *
     * @return CompletableFuture containing the transcribed text
     * @throws VoiceException if listening fails to start
     */
    CompletableFuture<String> startListening() throws VoiceException;

    /**
     * Stops listening for voice input.
     *
     * <p>This method cancels any active listening operation
     * and releases audio resources.</p>
     */
    void stopListening();

    /**
     * Checks if the voice system is currently listening for input.
     *
     * @return true if actively listening
     */
    boolean isListening();

    /**
     * Speaks the given text using text-to-speech.
     *
     * <p>This method should be non-blocking and return immediately.
     * Speech synthesis happens in the background.</p>
     *
     * @param text the text to speak
     * @throws VoiceException if speech synthesis fails
     */
    void speak(String text) throws VoiceException;

    /**
     * Stops any currently playing speech.
     *
     * <p>This method cancels active TTS output and should
     * return immediately.</p>
     */
    void stopSpeaking();

    /**
     * Checks if the voice system is currently speaking.
     *
     * @return true if currently speaking
     */
    boolean isSpeaking();

    /**
     * Performs a test of the voice system.
     *
     * <p>This method tests both STT and TTS functionality
     * and returns a result indicating success or failure.</p>
     *
     * @return CompletableFuture with test result
     */
    CompletableFuture<VoiceTestResult> test();

    /**
     * Shuts down the voice system and releases all resources.
     *
     * <p>This method should be called during mod cleanup.</p>
     */
    void shutdown();

    /**
     * Returns the speech-to-text subsystem.
     *
     * @return SpeechToText implementation
     */
    SpeechToText getSpeechToText();

    /**
     * Returns the text-to-speech subsystem.
     *
     * @return TextToSpeech implementation
     */
    TextToSpeech getTextToSpeech();

    /**
     * Result of a voice system test.
     */
    record VoiceTestResult(
        boolean success,
        String message,
        long latencyMs
    ) {
        public static VoiceTestResult success(String message, long latencyMs) {
            return new VoiceTestResult(true, message, latencyMs);
        }

        public static VoiceTestResult failure(String message) {
            return new VoiceTestResult(false, message, 0);
        }
    }
}
