package com.minewright.voice;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for text-to-speech (TTS) functionality.
 *
 * <p>Implementations convert text into spoken audio output,
 * allowing MineWright AI agents to communicate verbally with players.</p>
 *
 * <p><b>Configuration:</b></p>
 * TTS behavior can be configured via {@link VoiceConfig} including:
 * <ul>
 *   <li>Voice selection (different voices/accents)</li>
 *   <li>Speech rate (speed)</li>
 *   <li>Pitch adjustment</li>
 *   <li>Volume</li>
 * </ul>
 *
 * @since 1.2.0
 */
public interface TextToSpeech {

    /**
     * Initializes the text-to-speech system.
     *
     * <p>This method prepares the TTS engine, loads voice models,
     * and sets up audio output resources.</p>
     *
     * @throws VoiceException if initialization fails
     */
    void initialize() throws VoiceException;

    /**
     * Speaks the given text immediately.
     *
     * <p>This method is non-blocking and returns immediately.
     * Speech synthesis and playback happen in the background.</p>
     *
     * <p>If speech is currently playing, it will be interrupted
     * unless {@link #speakQueued(String)} is used.</p>
     *
     * @param text the text to speak
     * @throws VoiceException if speech synthesis fails
     */
    void speak(String text) throws VoiceException;

    /**
     * Adds text to the speech queue to be spoken after current speech completes.
     *
     * <p>This method is non-blocking and returns immediately.
     * The text will be spoken after any currently playing speech completes.</p>
     *
     * @param text the text to speak
     * @throws VoiceException if queuing fails
     */
    void speakQueued(String text) throws VoiceException;

    /**
     * Speaks the given text and returns a future that completes when finished.
     *
     * <p>This is a convenience method for waiting for speech to complete.</p>
     *
     * @param text the text to speak
     * @return CompletableFuture that completes when speech finishes
     * @throws VoiceException if speech synthesis fails
     */
    CompletableFuture<Void> speakAsync(String text) throws VoiceException;

    /**
     * Stops any currently playing speech and clears the speech queue.
     *
     * <p>This method returns immediately and cancels all pending speech.</p>
     */
    void stop();

    /**
     * Checks if the TTS system is currently speaking.
     *
     * @return true if currently speaking
     */
    boolean isSpeaking();

    /**
     * Checks if there is speech queued to be played.
     *
     * @return true if speech is queued
     */
    boolean hasQueuedSpeech();

    /**
     * Clears all queued speech without stopping current speech.
     *
     * <p>Use {@link #stop()} to stop current speech and clear the queue.</p>
     */
    void clearQueue();

    /**
     * Returns a list of available voices for this TTS implementation.
     *
     * <p>Each voice has a name and optionally language/locale information.</p>
     *
     * @return list of available voices
     */
    List<Voice> getAvailableVoices();

    /**
     * Returns the currently selected voice.
     *
     * @return current voice, or null if none selected
     */
    Voice getCurrentVoice();

    /**
     * Sets the voice to use for speech synthesis.
     *
     * @param voice the voice to use
     * @throws VoiceException if the voice is not available
     */
    void setVoice(Voice voice) throws VoiceException;

    /**
     * Sets the voice by name.
     *
     * <p>This is a convenience method that looks up the voice by name
     * and calls {@link #setVoice(Voice)}.</p>
     *
     * @param voiceName the name of the voice to use
     * @throws VoiceException if the voice is not found
     */
    void setVoice(String voiceName) throws VoiceException;

    /**
     * Returns the current speech rate (speed).
     *
     * @return rate multiplier (1.0 = normal, 2.0 = 2x speed, 0.5 = half speed)
     */
    double getRate();

    /**
     * Sets the speech rate (speed).
     *
     * @param rate rate multiplier (0.5 to 2.0, 1.0 = normal)
     */
    void setRate(double rate);

    /**
     * Returns the current pitch adjustment.
     *
     * @return pitch multiplier (1.0 = normal, higher = higher pitch)
     */
    double getPitch();

    /**
     * Sets the pitch adjustment.
     *
     * @param pitch pitch multiplier (0.5 to 2.0, 1.0 = normal)
     */
    void setPitch(double pitch);

    /**
     * Returns the current volume level.
     *
     * @return volume (0.0 to 1.0)
     */
    double getVolume();

    /**
     * Sets the volume level.
     *
     * @param volume volume (0.0 to 1.0)
     */
    void setVolume(double volume);

    /**
     * Shuts down the TTS system and releases all resources.
     */
    void shutdown();

    /**
     * Represents a voice available for text-to-speech.
     */
    record Voice(
        String name,
        String displayName,
        String language,
        String gender
    ) {
        public Voice {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Voice name cannot be null or blank");
            }
        }

        /**
         * Creates a voice with minimal information.
         */
        public static Voice of(String name, String displayName) {
            return new Voice(name, displayName, "en-US", "unknown");
        }

        /**
         * Creates a voice with full information.
         */
        public static Voice of(String name, String displayName, String language, String gender) {
            return new Voice(name, displayName, language, gender);
        }
    }
}
