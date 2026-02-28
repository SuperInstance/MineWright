package com.minewright.voice;

import com.minewright.testutil.TestLogger;
import com.minewright.config.MineWrightConfig;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;

/**
 * Manages the voice system instance and provides high-level voice operations.
 *
 * <p>This singleton class handles:
 * <ul>
 *   <li>Creating and initializing the appropriate voice system implementation</li>
 *   <li>Providing easy access to voice operations</li>
 *   <li>Managing voice system lifecycle (init, shutdown)</li>
 *   <li>Handling configuration changes</li>
 * </ul>
 *
 * <p><b>Usage:</b></p>
 * <pre>{@code
 * VoiceManager voice = VoiceManager.getInstance();
 *
 * // Check if voice is enabled
 * if (voice.isEnabled()) {
 *     // Listen for voice command
 *     voice.listenForCommand().thenAccept(text -> {
 *         processCommand(text);
 *     });
 *
 *     // Speak response
 *     voice.speak("Task completed!");
 * }
 * }</pre>
 *
 * @since 1.2.0
 */
public class VoiceManager {
    private static final Logger LOGGER = TestLogger.getLogger(VoiceManager.class);

    private static VoiceManager instance;

    private final VoiceSystem voiceSystem;
    private final VoiceConfig config;
    private boolean initialized = false;

    private VoiceManager() {
        this.config = new VoiceConfig();
        this.voiceSystem = createVoiceSystem();
    }

    /**
     * Returns the singleton VoiceManager instance.
     *
     * @return VoiceManager instance
     */
    public static VoiceManager getInstance() {
        if (instance == null) {
            instance = new VoiceManager();
        }
        return instance;
    }

    /**
     * Initializes the voice system.
     *
     * <p>This method should be called during mod initialization.
     * It loads configuration and initializes the appropriate voice system.</p>
     *
     * @throws VoiceException if initialization fails
     */
    public void initialize() throws VoiceException {
        if (initialized) {
            LOGGER.debug("Voice system already initialized");
            return;
        }

        LOGGER.info("Initializing voice system...");
        config.loadFromMineWrightConfig();

        if (!config.isEnabled()) {
            LOGGER.info("Voice system is disabled in configuration");
            initialized = true;
            return;
        }

        try {
            voiceSystem.initialize();
            applyConfiguration();
            initialized = true;
            LOGGER.info("Voice system initialized successfully in {} mode", config.getMode());
        } catch (VoiceException e) {
            LOGGER.error("Failed to initialize voice system", e);
            throw e;
        }
    }

    /**
     * Checks if voice functionality is enabled.
     *
     * @return true if voice is enabled
     */
    public boolean isEnabled() {
        return initialized && config.isEnabled() && voiceSystem.isEnabled();
    }

    /**
     * Enables or disables voice functionality.
     *
     * @param enabled true to enable, false to disable
     */
    public void setEnabled(boolean enabled) {
        LOGGER.info("Voice system enabled: {}", enabled);
        config.setEnabled(enabled);
        voiceSystem.setEnabled(enabled);
    }

    /**
     * Listens for a voice command and returns the transcribed text.
     *
     * <p>This is a convenience method that starts listening and
     * returns a future with the complete transcription.</p>
     *
     * @return CompletableFuture containing the transcribed command
     * @throws VoiceException if listening fails to start
     */
    public CompletableFuture<String> listenForCommand() throws VoiceException {
        if (!isEnabled()) {
            throw VoiceException.configurationError("Voice system is not enabled");
        }

        LOGGER.info("Listening for voice command...");
        return voiceSystem.startListening();
    }

    /**
     * Speaks the given text using text-to-speech.
     *
     * @param text the text to speak
     */
    public void speak(String text) {
        if (!isEnabled()) {
            LOGGER.debug("Voice system disabled - not speaking: {}", text);
            return;
        }

        try {
            voiceSystem.speak(text);
        } catch (VoiceException e) {
            LOGGER.error("Failed to speak: {}", text, e);
        }
    }

    /**
     * Speaks the given text only if TTS is enabled.
     *
     * <p>This is a convenience method that silently fails if voice is disabled.</p>
     *
     * @param text the text to speak
     */
    public void speakIfEnabled(String text) {
        speak(text);
    }

    /**
     * Stops any current listening or speaking operations.
     */
    public void stopAll() {
        voiceSystem.stopListening();
        voiceSystem.stopSpeaking();
    }

    /**
     * Tests the voice system.
     *
     * @return CompletableFuture with test result
     */
    public CompletableFuture<VoiceSystem.VoiceTestResult> test() {
        LOGGER.info("Running voice system test...");
        return voiceSystem.test();
    }

    /**
     * Returns the underlying voice system implementation.
     *
     * @return VoiceSystem instance
     */
    public VoiceSystem getVoiceSystem() {
        return voiceSystem;
    }

    /**
     * Returns the voice configuration.
     *
     * @return VoiceConfig instance
     */
    public VoiceConfig getConfig() {
        return config;
    }

    /**
     * Reloads voice configuration and applies changes.
     */
    public void reloadConfiguration() {
        LOGGER.info("Reloading voice configuration...");
        config.loadFromMineWrightConfig();
        applyConfiguration();
    }

    /**
     * Shuts down the voice system and releases all resources.
     *
     * <p>This method should be called during mod cleanup.</p>
     */
    public void shutdown() {
        LOGGER.info("Shutting down voice system...");
        voiceSystem.shutdown();
        initialized = false;
    }

    /**
     * Creates the appropriate voice system implementation based on configuration.
     */
    private VoiceSystem createVoiceSystem() {
        String mode = MineWrightConfig.getValidatedVoiceMode();

        return switch (mode.toLowerCase()) {
            case "logging" -> {
                LOGGER.info("Creating logging voice system (for testing)");
                yield new LoggingVoiceSystem();
            }
            case "real" -> {
                LOGGER.info("Creating real voice system with Whisper STT");
                yield new RealVoiceSystem();
            }
            case "disabled", "off", "false" -> {
                LOGGER.info("Voice system disabled");
                yield new DisabledVoiceSystem();
            }
            default -> {
                LOGGER.warn("Unknown voice mode '{}' - using disabled mode", mode);
                yield new DisabledVoiceSystem();
            }
        };
    }

    /**
     * Applies configuration settings to the voice system.
     */
    private void applyConfiguration() {
        // Apply STT settings
        var stt = voiceSystem.getSpeechToText();
        stt.setLanguage(config.getSttLanguage());
        stt.setSensitivity(config.getSttSensitivity());

        // Apply TTS settings
        var tts = voiceSystem.getTextToSpeech();
        tts.setVolume(config.getTtsVolume());
        tts.setRate(config.getTtsRate());
        tts.setPitch(config.getTtsPitch());

        try {
            tts.setVoice(config.getTtsVoice());
        } catch (VoiceException e) {
            LOGGER.warn("Failed to set TTS voice: {}", config.getTtsVoice(), e);
        }
    }
}
