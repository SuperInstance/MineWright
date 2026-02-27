package com.minewright.voice;

import com.minewright.MineWrightMod;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;

/**
 * No-op implementation of VoiceSystem for when voice is disabled.
 *
 * <p>This implementation provides stub implementations of all voice
 * functionality that do nothing and return immediately. It is used
 * when voice features are disabled via configuration or when the
 * voice system is not available.</p>
 *
 * <p>All methods log debug messages and return safe default values.</p>
 *
 * @since 1.2.0
 */
public class DisabledVoiceSystem implements VoiceSystem {

    private static final Logger LOGGER = MineWrightMod.LOGGER;

    @Override
    public void initialize() throws VoiceException {
        LOGGER.debug("Voice system is disabled - skipping initialization");
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public void setEnabled(boolean enabled) {
        LOGGER.debug("Cannot enable voice system - no voice implementation available");
    }

    @Override
    public CompletableFuture<String> startListening() throws VoiceException {
        LOGGER.debug("Voice system is disabled - startListening() ignored");
        return CompletableFuture.failedFuture(
            VoiceException.configurationError("Voice system is disabled")
        );
    }

    @Override
    public void stopListening() {
        LOGGER.debug("Voice system is disabled - stopListening() ignored");
    }

    @Override
    public boolean isListening() {
        return false;
    }

    @Override
    public void speak(String text) throws VoiceException {
        LOGGER.debug("Voice system is disabled - speak() ignored: {}", text);
    }

    @Override
    public void stopSpeaking() {
        LOGGER.debug("Voice system is disabled - stopSpeaking() ignored");
    }

    @Override
    public boolean isSpeaking() {
        return false;
    }

    @Override
    public CompletableFuture<VoiceTestResult> test() {
        LOGGER.debug("Voice system is disabled - test() failed");
        return CompletableFuture.completedFuture(
            VoiceTestResult.failure("Voice system is disabled")
        );
    }

    @Override
    public void shutdown() {
        LOGGER.debug("Voice system is disabled - shutdown() ignored");
    }

    @Override
    public SpeechToText getSpeechToText() {
        return new DisabledSpeechToText();
    }

    @Override
    public TextToSpeech getTextToSpeech() {
        return new DisabledTextToSpeech();
    }

    /**
     * No-op SpeechToText implementation.
     */
    private static class DisabledSpeechToText implements SpeechToText {

        @Override
        public void initialize() throws VoiceException {
            LOGGER.debug("STT is disabled - initialize() ignored");
        }

        @Override
        public void startListening(java.util.function.Consumer<String> resultConsumer) throws VoiceException {
            LOGGER.debug("STT is disabled - startListening() ignored");
        }

        @Override
        public void stopListening() {
            LOGGER.debug("STT is disabled - stopListening() ignored");
        }

        @Override
        public boolean isListening() {
            return false;
        }

        @Override
        public CompletableFuture<String> listenOnce() throws VoiceException {
            LOGGER.debug("STT is disabled - listenOnce() failed");
            return CompletableFuture.failedFuture(
                VoiceException.configurationError("Speech-to-text is disabled")
            );
        }

        @Override
        public void cancel() {
            LOGGER.debug("STT is disabled - cancel() ignored");
        }

        @Override
        public javax.sound.sampled.AudioFormat getPreferredAudioFormat() {
            // Return a standard audio format
            return new javax.sound.sampled.AudioFormat(
                16000.0f,  // Sample rate
                16,        // Sample size in bits
                1,         // Channels (mono)
                true,      // Signed
                false      // Big-endian
            );
        }

        @Override
        public String getLanguage() {
            return "en-US";
        }

        @Override
        public void setLanguage(String language) {
            LOGGER.debug("STT is disabled - setLanguage() ignored: {}", language);
        }

        @Override
        public double getSensitivity() {
            return 0.5;
        }

        @Override
        public void setSensitivity(double sensitivity) {
            LOGGER.debug("STT is disabled - setSensitivity() ignored: {}", sensitivity);
        }

        @Override
        public void shutdown() {
            LOGGER.debug("STT is disabled - shutdown() ignored");
        }
    }

    /**
     * No-op TextToSpeech implementation.
     */
    private static class DisabledTextToSpeech implements TextToSpeech {

        @Override
        public void initialize() throws VoiceException {
            LOGGER.debug("TTS is disabled - initialize() ignored");
        }

        @Override
        public void speak(String text) throws VoiceException {
            LOGGER.debug("TTS is disabled - speak() ignored: {}", text);
        }

        @Override
        public void speakQueued(String text) throws VoiceException {
            LOGGER.debug("TTS is disabled - speakQueued() ignored: {}", text);
        }

        @Override
        public CompletableFuture<Void> speakAsync(String text) throws VoiceException {
            LOGGER.debug("TTS is disabled - speakAsync() completed immediately: {}", text);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void stop() {
            LOGGER.debug("TTS is disabled - stop() ignored");
        }

        @Override
        public boolean isSpeaking() {
            return false;
        }

        @Override
        public boolean hasQueuedSpeech() {
            return false;
        }

        @Override
        public void clearQueue() {
            LOGGER.debug("TTS is disabled - clearQueue() ignored");
        }

        @Override
        public java.util.List<Voice> getAvailableVoices() {
            return java.util.List.of();
        }

        @Override
        public Voice getCurrentVoice() {
            return null;
        }

        @Override
        public void setVoice(Voice voice) throws VoiceException {
            LOGGER.debug("TTS is disabled - setVoice() ignored: {}", voice != null ? voice.name() : null);
        }

        @Override
        public void setVoice(String voiceName) throws VoiceException {
            LOGGER.debug("TTS is disabled - setVoice() ignored: {}", voiceName);
        }

        @Override
        public double getRate() {
            return 1.0;
        }

        @Override
        public void setRate(double rate) {
            LOGGER.debug("TTS is disabled - setRate() ignored: {}", rate);
        }

        @Override
        public double getPitch() {
            return 1.0;
        }

        @Override
        public void setPitch(double pitch) {
            LOGGER.debug("TTS is disabled - setPitch() ignored: {}", pitch);
        }

        @Override
        public double getVolume() {
            return 1.0;
        }

        @Override
        public void setVolume(double volume) {
            LOGGER.debug("TTS is disabled - setVolume() ignored: {}", volume);
        }

        @Override
        public void shutdown() {
            LOGGER.debug("TTS is disabled - shutdown() ignored");
        }
    }
}
