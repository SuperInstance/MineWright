package com.minewright.voice;

import com.minewright.testutil.TestLogger;
import com.minewright.config.ConfigChangeEvent;
import com.minewright.config.ConfigChangeListener;
import com.minewright.config.MineWrightConfig;
import com.minewright.exception.ConfigException;
import org.slf4j.Logger;

/**
 * Configuration holder for voice system settings.
 *
 * <p>This class loads and caches voice configuration from MineWrightConfig,
 * providing a convenient API for accessing voice-related settings.</p>
 *
 * <p>This class implements {@link ConfigChangeListener} to automatically
 * reload configuration when the config file is changed.</p>
 *
 * @since 1.2.0
 */
public class VoiceConfig implements ConfigChangeListener {
    private static final Logger LOGGER = TestLogger.getLogger(VoiceConfig.class);

    private boolean enabled;
    private String mode;
    private String sttLanguage;
    private String ttsVoice;
    private double ttsVolume;
    private double ttsRate;
    private double ttsPitch;
    private double sttSensitivity;
    private boolean pushToTalk;
    private int listeningTimeout;
    private boolean debugLogging;

    /**
     * Creates a new VoiceConfig with default values.
     */
    public VoiceConfig() {
        loadFromMineWrightConfig();
    }

    /**
     * Loads configuration values from MineWrightConfig.
     *
     * <p>This method reads the current values from MineWrightConfig
     * and updates this instance's cached values.</p>
     */
    public void loadFromMineWrightConfig() {
        this.enabled = MineWrightConfig.VOICE_ENABLED.get();
        this.mode = MineWrightConfig.VOICE_MODE.get();
        this.sttLanguage = MineWrightConfig.VOICE_STT_LANGUAGE.get();
        this.ttsVoice = MineWrightConfig.VOICE_TTS_VOICE.get();
        this.ttsVolume = MineWrightConfig.VOICE_TTS_VOLUME.get();
        this.ttsRate = MineWrightConfig.VOICE_TTS_RATE.get();
        this.ttsPitch = MineWrightConfig.VOICE_TTS_PITCH.get();
        this.sttSensitivity = MineWrightConfig.VOICE_STT_SENSITIVITY.get();
        this.pushToTalk = MineWrightConfig.VOICE_PUSH_TO_TALK.get();
        this.listeningTimeout = MineWrightConfig.VOICE_LISTENING_TIMEOUT.get();
        this.debugLogging = MineWrightConfig.VOICE_DEBUG_LOGGING.get();
    }

    // Getters
    public boolean isEnabled() {
        return enabled;
    }

    public String getMode() {
        return mode;
    }

    public String getSttLanguage() {
        return sttLanguage;
    }

    public String getTtsVoice() {
        return ttsVoice;
    }

    public double getTtsVolume() {
        return ttsVolume;
    }

    public double getTtsRate() {
        return ttsRate;
    }

    public double getTtsPitch() {
        return ttsPitch;
    }

    public double getSttSensitivity() {
        return sttSensitivity;
    }

    public boolean isPushToTalk() {
        return pushToTalk;
    }

    public int getListeningTimeout() {
        return listeningTimeout;
    }

    public boolean isDebugLogging() {
        return debugLogging;
    }

    // Setters (for programmatic configuration)
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public void setSttLanguage(String sttLanguage) {
        this.sttLanguage = sttLanguage;
    }

    public void setTtsVoice(String ttsVoice) {
        this.ttsVoice = ttsVoice;
    }

    public void setTtsVolume(double ttsVolume) {
        this.ttsVolume = ttsVolume;
    }

    public void setTtsRate(double ttsRate) {
        this.ttsRate = ttsRate;
    }

    public void setTtsPitch(double ttsPitch) {
        this.ttsPitch = ttsPitch;
    }

    public void setSttSensitivity(double sttSensitivity) {
        this.sttSensitivity = sttSensitivity;
    }

    public void setPushToTalk(boolean pushToTalk) {
        this.pushToTalk = pushToTalk;
    }

    public void setListeningTimeout(int listeningTimeout) {
        this.listeningTimeout = listeningTimeout;
    }

    public void setDebugLogging(boolean debugLogging) {
        this.debugLogging = debugLogging;
    }

    @Override
    public String toString() {
        return "VoiceConfig{" +
            "enabled=" + enabled +
            ", mode='" + mode + '\'' +
            ", sttLanguage='" + sttLanguage + '\'' +
            ", ttsVoice='" + ttsVoice + '\'' +
            ", ttsVolume=" + ttsVolume +
            ", ttsRate=" + ttsRate +
            ", ttsPitch=" + ttsPitch +
            ", sttSensitivity=" + sttSensitivity +
            ", pushToTalk=" + pushToTalk +
            ", listeningTimeout=" + listeningTimeout +
            ", debugLogging=" + debugLogging +
            '}';
    }

    // ========================================================================
    // ConfigChangeListener Implementation
    // ========================================================================

    /**
     * Called before configuration is reloaded.
     *
     * <p>Stops any active voice operations to prepare for config changes.</p>
     */
    @Override
    public void onConfigReloading() {
        LOGGER.debug("VoiceConfig: Preparing for config reload");
        // Voice operations will be reconfigured after reload
    }

    /**
     * Called when configuration values have been reloaded.
     *
     * <p>Reloads all voice configuration values from MineWrightConfig
     * and applies them to the voice system.</p>
     *
     * @param event The config change event
     */
    @Override
    public void onConfigChanged(ConfigChangeEvent event) {
        if (event.affects("voice")) {
            LOGGER.info("VoiceConfig: Reloading voice configuration");

            boolean wasEnabled = this.enabled;
            loadFromMineWrightConfig();

            LOGGER.info("Voice configuration reloaded: enabled={}, mode={}",
                this.enabled, this.mode);

            // Log significant changes
            if (wasEnabled && !this.enabled) {
                LOGGER.info("Voice system has been disabled via config reload");
            } else if (!wasEnabled && this.enabled) {
                LOGGER.info("Voice system has been enabled via config reload");
            }
        }
    }

    /**
     * Called when configuration reload fails.
     *
     * <p>Logs the error and keeps the existing configuration.</p>
     *
     * @param exception The exception that caused the reload to fail
     */
    @Override
    public void onConfigReloadFailed(com.minewright.exception.ConfigException exception) {
        LOGGER.warn("VoiceConfig: Config reload failed, keeping existing configuration: {}",
            exception.getMessage());
        // Keep existing configuration values
    }

    /**
     * Validates the current voice configuration.
     *
     * @return true if voice configuration is valid
     */
    public boolean isValid() {
        // Validate voice mode
        String mode = getMode();
        if (mode == null || mode.trim().isEmpty()) {
            LOGGER.warn("Voice mode is empty");
            return false;
        }

        String lowerMode = mode.toLowerCase();
        if (!lowerMode.equals("disabled") &&
            !lowerMode.equals("logging") &&
            !lowerMode.equals("real")) {
            LOGGER.warn("Invalid voice mode: {}", mode);
            return false;
        }

        // Validate numeric ranges
        if (getTtsVolume() < 0.0 || getTtsVolume() > 1.0) {
            LOGGER.warn("TTS volume out of range: {}", getTtsVolume());
            return false;
        }

        if (getTtsRate() < 0.5 || getTtsRate() > 2.0) {
            LOGGER.warn("TTS rate out of range: {}", getTtsRate());
            return false;
        }

        if (getTtsPitch() < 0.5 || getTtsPitch() > 2.0) {
            LOGGER.warn("TTS pitch out of range: {}", getTtsPitch());
            return false;
        }

        if (getSttSensitivity() < 0.0 || getSttSensitivity() > 1.0) {
            LOGGER.warn("STT sensitivity out of range: {}", getSttSensitivity());
            return false;
        }

        if (getListeningTimeout() < 0 || getListeningTimeout() > 60) {
            LOGGER.warn("Listening timeout out of range: {}", getListeningTimeout());
            return false;
        }

        return true;
    }

    /**
     * Gets a summary of the current voice configuration.
     *
     * @return Configuration summary string
     */
    public String getSummary() {
        return String.format(
            "Voice[enabled=%s, mode=%s, language=%s, volume=%.2f, rate=%.2f, pitch=%.2f]",
            enabled, mode, sttLanguage, ttsVolume, ttsRate, ttsPitch
        );
    }
}
