package com.minewright.voice;

import com.minewright.MineWrightMod;
import org.slf4j.Logger;

import javax.sound.sampled.AudioFormat;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Logging implementation of VoiceSystem for testing voice integration.
 *
 * <p>This implementation logs all voice operations instead of actually
 * performing speech-to-text or text-to-speech. It is useful for:
 * <ul>
 *   <li>Testing voice command flows without audio hardware</li>
 *   <li>Verifying voice system integration points</li>
 *   <li>Debugging voice-related code</li>
 *   <li>Development when actual voice APIs are not configured</li>
 * </ul>
 *
 * <p>All operations log what would happen with a real voice system,
 * and {@link #startListening()} returns a simulated transcription
 * after a short delay.</p>
 *
 * @since 1.2.0
 */
public class LoggingVoiceSystem implements VoiceSystem {

    private static final Logger LOGGER = MineWrightMod.LOGGER;

    private final LoggingSpeechToText stt;
    private final LoggingTextToSpeech tts;
    private boolean enabled = true;

    public LoggingVoiceSystem() {
        this.stt = new LoggingSpeechToText();
        this.tts = new LoggingTextToSpeech();
    }

    @Override
    public void initialize() throws VoiceException {
        LOGGER.info("[VOICE] Initializing logging voice system");
        stt.initialize();
        tts.initialize();
        LOGGER.info("[VOICE] Logging voice system initialized successfully");
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        LOGGER.info("[VOICE] Voice system enabled: {}", enabled);
        this.enabled = enabled;
    }

    @Override
    public CompletableFuture<String> startListening() throws VoiceException {
        LOGGER.info("[VOICE] Starting to listen for voice input...");
        return stt.listenOnce();
    }

    @Override
    public void stopListening() {
        LOGGER.info("[VOICE] Stopping voice input listening");
        stt.stopListening();
    }

    @Override
    public boolean isListening() {
        return stt.isListening();
    }

    @Override
    public void speak(String text) throws VoiceException {
        LOGGER.info("[VOICE] Speaking: \"{}\"", text);
        tts.speak(text);
    }

    @Override
    public void stopSpeaking() {
        LOGGER.info("[VOICE] Stopping speech output");
        tts.stop();
    }

    @Override
    public boolean isSpeaking() {
        return tts.isSpeaking();
    }

    @Override
    public CompletableFuture<VoiceTestResult> test() {
        LOGGER.info("[VOICE] Running voice system test...");

        long startTime = System.currentTimeMillis();

        // Simulate test delay
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long latency = System.currentTimeMillis() - startTime;
        LOGGER.info("[VOICE] Voice test completed (STT: OK, TTS: OK, Latency: {}ms)", latency);

        return CompletableFuture.completedFuture(
            VoiceTestResult.success("Voice system test passed", latency)
        );
    }

    @Override
    public void shutdown() {
        LOGGER.info("[VOICE] Shutting down voice system");
        stt.shutdown();
        tts.shutdown();
    }

    @Override
    public SpeechToText getSpeechToText() {
        return stt;
    }

    @Override
    public TextToSpeech getTextToSpeech() {
        return tts;
    }

    /**
     * Logging SpeechToText implementation that simulates transcription.
     */
    private static class LoggingSpeechToText implements SpeechToText {

        private boolean listening = false;

        @Override
        public void initialize() throws VoiceException {
            LOGGER.debug("[VOICE/STT] Initializing speech-to-text");
        }

        @Override
        public void startListening(Consumer<String> resultConsumer) throws VoiceException {
            LOGGER.info("[VOICE/STT] Starting continuous listening");
            listening = true;

            // Simulate receiving transcription after a delay
            CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(500);
                    if (listening) {
                        String simulated = "build a house";
                        LOGGER.info("[VOICE/STT] Transcribed: \"{}\"", simulated);
                        resultConsumer.accept(simulated);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        @Override
        public void stopListening() {
            LOGGER.info("[VOICE/STT] Stopping listening");
            listening = false;
        }

        @Override
        public boolean isListening() {
            return listening;
        }

        @Override
        public CompletableFuture<String> listenOnce() throws VoiceException {
            LOGGER.info("[VOICE/STT] Starting single-shot listening");

            return CompletableFuture.supplyAsync(() -> {
                try {
                    // Simulate processing delay
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                String result = "build a house";
                LOGGER.info("[VOICE/STT] Transcribed: \"{}\"", result);
                return result;
            });
        }

        @Override
        public void cancel() {
            LOGGER.info("[VOICE/STT] Cancelling listening operation");
            listening = false;
        }

        @Override
        public AudioFormat getPreferredAudioFormat() {
            return new AudioFormat(16000.0f, 16, 1, true, false);
        }

        @Override
        public String getLanguage() {
            return "en-US";
        }

        @Override
        public void setLanguage(String language) {
            LOGGER.debug("[VOICE/STT] Language set to: {}", language);
        }

        @Override
        public double getSensitivity() {
            return 0.5;
        }

        @Override
        public void setSensitivity(double sensitivity) {
            LOGGER.debug("[VOICE/STT] Sensitivity set to: {}", sensitivity);
        }

        @Override
        public void shutdown() {
            LOGGER.debug("[VOICE/STT] Shutting down");
            listening = false;
        }
    }

    /**
     * Logging TextToSpeech implementation that logs what would be spoken.
     */
    private static class LoggingTextToSpeech implements TextToSpeech {

        private boolean speaking = false;
        private double volume = 1.0;
        private double rate = 1.0;
        private double pitch = 1.0;

        @Override
        public void initialize() throws VoiceException {
            LOGGER.debug("[VOICE/TTS] Initializing text-to-speech");
        }

        @Override
        public void speak(String text) throws VoiceException {
            LOGGER.info("[VOICE/TTS] Speaking: \"{}\"", text);
            speaking = true;

            // Simulate speech completing after a delay
            CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                speaking = false;
                LOGGER.debug("[VOICE/TTS] Speech completed");
            });
        }

        @Override
        public void speakQueued(String text) throws VoiceException {
            LOGGER.info("[VOICE/TTS] Queued: \"{}\"", text);
        }

        @Override
        public CompletableFuture<Void> speakAsync(String text) throws VoiceException {
            LOGGER.info("[VOICE/TTS] Speaking async: \"{}\"", text);
            speaking = true;

            return CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                speaking = false;
                LOGGER.debug("[VOICE/TTS] Async speech completed");
            });
        }

        @Override
        public void stop() {
            LOGGER.info("[VOICE/TTS] Stopping speech and clearing queue");
            speaking = false;
        }

        @Override
        public boolean isSpeaking() {
            return speaking;
        }

        @Override
        public boolean hasQueuedSpeech() {
            return false;
        }

        @Override
        public void clearQueue() {
            LOGGER.debug("[VOICE/TTS] Clearing speech queue");
        }

        @Override
        public List<Voice> getAvailableVoices() {
            return List.of(
                Voice.of("default", "Default Voice", "en-US", "neutral"),
                Voice.of("steve", "Steve Voice", "en-US", "male"),
                Voice.of("alex", "Alex Voice", "en-US", "female")
            );
        }

        @Override
        public Voice getCurrentVoice() {
            return Voice.of("default", "Default Voice", "en-US", "neutral");
        }

        @Override
        public void setVoice(Voice voice) throws VoiceException {
            LOGGER.info("[VOICE/TTS] Voice changed to: {} ({})", voice.displayName(), voice.name());
        }

        @Override
        public void setVoice(String voiceName) throws VoiceException {
            LOGGER.info("[VOICE/TTS] Voice changed to: {}", voiceName);
        }

        @Override
        public double getRate() {
            return rate;
        }

        @Override
        public void setRate(double rate) {
            LOGGER.debug("[VOICE/TTS] Rate set to: {}", rate);
            this.rate = rate;
        }

        @Override
        public double getPitch() {
            return pitch;
        }

        @Override
        public void setPitch(double pitch) {
            LOGGER.debug("[VOICE/TTS] Pitch set to: {}", pitch);
            this.pitch = pitch;
        }

        @Override
        public double getVolume() {
            return volume;
        }

        @Override
        public void setVolume(double volume) {
            LOGGER.debug("[VOICE/TTS] Volume set to: {}", volume);
            this.volume = volume;
        }

        @Override
        public void shutdown() {
            LOGGER.debug("[VOICE/TTS] Shutting down");
            speaking = false;
        }
    }
}
