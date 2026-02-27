package com.minewright.voice;

import com.minewright.config.MineWrightConfig;

/**
 * Configuration holder for voice system settings.
 *
 * <p>This class loads and caches voice configuration from MineWrightConfig,
 * providing a convenient API for accessing voice-related settings.</p>
 *
 * @since 1.2.0
 */
public class VoiceConfig {

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
}
