package com.minewright.config;

import com.minewright.testutil.TestLogger;
import org.slf4j.Logger;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.Arrays;
import java.util.List;

/**
 * Configuration for voice input/output features.
 *
 * <h2>Configuration Section</h2>
 * <p><b>{@code [voice]}</b> - Voice integration settings</p>
 *
 * <h2>Voice Modes</h2>
 * <ul>
 *   <li><b>disabled</b> - Voice features completely disabled</li>
 *   <li><b>logging</b> - Logs what would be heard/said (for testing)</li>
 *   <li><b>real</b> - Actual TTS/STT functionality</li>
 * </ul>
 *
 * @since 3.0.0
 */
public class VoiceConfig {
    private static final Logger LOGGER = TestLogger.getLogger(VoiceConfig.class);
    private static final List<String> VALID_VOICE_MODES = Arrays.asList("disabled", "logging", "real");

    /** The Forge configuration spec */
    public static final ForgeConfigSpec SPEC;

    // ------------------------------------------------------------------------
    // Voice Configuration
    // ------------------------------------------------------------------------

    /**
     * Enable voice input/output features.
     * <p><b>Default:</b> {@code false}</p>
     * <p><b>Config key:</b> {@code voice.enabled}</p>
     */
    public static final ForgeConfigSpec.BooleanValue VOICE_ENABLED;

    /**
     * Voice system mode.
     * <p><b>Valid values:</b> {@code disabled}, {@code logging}, {@code real}</p>
     * <p><b>Default:</b> {@code logging}</p>
     * <p><b>Config key:</b> {@code voice.mode}</p>
     */
    public static final ForgeConfigSpec.ConfigValue<String> VOICE_MODE;

    /**
     * Speech-to-text language code.
     * <p><b>Default:</b> {@code en-US}</p>
     * <p><b>Config key:</b> {@code voice.sttLanguage}</p>
     */
    public static final ForgeConfigSpec.ConfigValue<String> VOICE_STT_LANGUAGE;

    /**
     * Text-to-speech voice name.
     * <p><b>Default:</b> {@code default}</p>
     * <p><b>Config key:</b> {@code voice.ttsVoice}</p>
     */
    public static final ForgeConfigSpec.ConfigValue<String> VOICE_TTS_VOICE;

    /**
     * TTS volume level.
     * <p><b>Range:</b> 0.0 to 1.0</p>
     * <p><b>Default:</b> 0.8</p>
     * <p><b>Config key:</b> {@code voice.ttsVolume}</p>
     */
    public static final ForgeConfigSpec.DoubleValue VOICE_TTS_VOLUME;

    /**
     * TTS speech rate.
     * <p><b>Range:</b> 0.5 to 2.0 (1.0 = normal)</p>
     * <p><b>Default:</b> 1.0</p>
     * <p><b>Config key:</b> {@code voice.ttsRate}</p>
     */
    public static final ForgeConfigSpec.DoubleValue VOICE_TTS_RATE;

    /**
     * TTS pitch adjustment.
     * <p><b>Range:</b> 0.5 to 2.0 (1.0 = normal)</p>
     * <p><b>Default:</b> 1.0</p>
     * <p><b>Config key:</b> {@code voice.ttsPitch}</p>
     */
    public static final ForgeConfigSpec.DoubleValue VOICE_TTS_PITCH;

    /**
     * STT sensitivity for speech detection.
     * <p><b>Range:</b> 0.0 to 1.0</p>
     * <p><b>Default:</b> 0.5</p>
     * <p><b>Config key:</b> {@code voice.sttSensitivity}</p>
     */
    public static final ForgeConfigSpec.DoubleValue VOICE_STT_SENSITIVITY;

    /**
     * Require push-to-talk key for voice input.
     * <p><b>Default:</b> {@code true}</p>
     * <p><b>Config key:</b> {@code voice.pushToTalk}</p>
     */
    public static final ForgeConfigSpec.BooleanValue VOICE_PUSH_TO_TALK;

    /**
     * Auto-stop listening after N seconds of silence.
     * <p><b>Range:</b> 0 to 60 (0 = no timeout)</p>
     * <p><b>Default:</b> 10</p>
     * <p><b>Config key:</b> {@code voice.listeningTimeout}</p>
     */
    public static final ForgeConfigSpec.IntValue VOICE_LISTENING_TIMEOUT;

    /**
     * Enable verbose logging for voice system operations.
     * <p><b>Default:</b> {@code true}</p>
     * <p><b>Config key:</b> {@code voice.debugLogging}</p>
     */
    public static final ForgeConfigSpec.BooleanValue VOICE_DEBUG_LOGGING;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Voice Integration Configuration").push("voice");

        VOICE_ENABLED = builder
            .comment("Enable voice input/output features")
            .define("enabled", false);

        VOICE_MODE = builder
            .comment("Voice system mode: 'disabled', 'logging' (logs what would be heard/said), or 'real' (actual TTS/STT)")
            .define("mode", "logging");

        VOICE_STT_LANGUAGE = builder
            .comment("Speech-to-text language (e.g., 'en-US', 'en-GB', 'es-ES')")
            .define("sttLanguage", "en-US");

        VOICE_TTS_VOICE = builder
            .comment("Text-to-speech voice name")
            .define("ttsVoice", "default");

        VOICE_TTS_VOLUME = builder
            .comment("TTS volume level (0.0 to 1.0)")
            .defineInRange("ttsVolume", 0.8, 0.0, 1.0);

        VOICE_TTS_RATE = builder
            .comment("TTS speech rate (0.5 to 2.0, 1.0 = normal)")
            .defineInRange("ttsRate", 1.0, 0.5, 2.0);

        VOICE_TTS_PITCH = builder
            .comment("TTS pitch adjustment (0.5 to 2.0, 1.0 = normal)")
            .defineInRange("ttsPitch", 1.0, 0.5, 2.0);

        VOICE_STT_SENSITIVITY = builder
            .comment("STT sensitivity for speech detection (0.0 to 1.0, higher = more sensitive)")
            .defineInRange("sttSensitivity", 0.5, 0.0, 1.0);

        VOICE_PUSH_TO_TALK = builder
            .comment("Require push-to-talk key for voice input (vs continuous listening)")
            .define("pushToTalk", true);

        VOICE_LISTENING_TIMEOUT = builder
            .comment("Auto-stop listening after N seconds of silence (0 = no timeout)")
            .defineInRange("listeningTimeout", 10, 0, 60);

        VOICE_DEBUG_LOGGING = builder
            .comment("Enable verbose logging for voice system operations")
            .define("debugLogging", true);

        builder.pop();

        SPEC = builder.build();
    }

    /**
     * Validates the voice configuration.
     *
     * @return true if configuration is valid
     */
    public static boolean validate() {
        LOGGER.info("Validating voice configuration...");

        // Validate voice mode
        String voiceMode = VOICE_MODE.get();
        if (voiceMode != null && !VALID_VOICE_MODES.contains(voiceMode.toLowerCase())) {
            LOGGER.warn("Invalid voice mode '{}'. Valid options: {}. Defaulting to 'disabled'.",
                voiceMode, VALID_VOICE_MODES);
            return false;
        }

        // Log voice status
        if (VOICE_ENABLED.get()) {
            LOGGER.info("Voice system: enabled (mode: {})", VOICE_MODE.get());
        } else {
            LOGGER.info("Voice system: disabled");
        }

        return true;
    }

    /**
     * Checks if voice is properly configured and enabled.
     *
     * @return true if voice is enabled
     */
    public static boolean isVoiceEnabled() {
        return VOICE_ENABLED.get();
    }

    /**
     * Gets the validated voice mode.
     *
     * @return The voice mode (never null)
     */
    public static String getValidatedVoiceMode() {
        String mode = VOICE_MODE.get();
        if (mode == null || mode.trim().isEmpty()) {
            return "disabled";
        }
        String lowerMode = mode.toLowerCase();
        return VALID_VOICE_MODES.contains(lowerMode) ? lowerMode : "disabled";
    }

    /**
     * Gets a configuration summary for logging/debugging.
     *
     * @return Summary string of current voice configuration
     */
    public static String getConfigSummary() {
        return String.format(
            "VoiceConfig[enabled=%s, mode=%s, sttLanguage=%s, ttsVoice=%s, volume=%.2f, rate=%.2f]",
            VOICE_ENABLED.get(),
            VOICE_MODE.get(),
            VOICE_STT_LANGUAGE.get(),
            VOICE_TTS_VOICE.get(),
            VOICE_TTS_VOLUME.get(),
            VOICE_TTS_RATE.get()
        );
    }
}
