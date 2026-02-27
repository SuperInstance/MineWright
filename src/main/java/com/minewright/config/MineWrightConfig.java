package com.minewright.config;

import com.minewright.MineWrightMod;
import com.minewright.exception.ConfigException;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.Arrays;
import java.util.List;

/**
 * Configuration for MineWright mod.
 *
 * <h2>Config File Location</h2>
 * <p><code>config/minewright-common.toml</code></p>
 *
 * <p><b>Platform-specific paths:</b></p>
 * <ul>
 *   <li><b>Windows:</b> <code>.minecraft/config/minewright-common.toml</code></li>
 *   <li><b>Linux/Mac:</b> <code>~/.minecraft/config/minewright-common.toml</code></li>
 * </ul>
 *
 * <h2>Config Reload</h2>
 * <p>Edit the config file and use <code>/reload</code> to reload without restart.</p>
 *
 * <h2>Configuration Sections</h2>
 * <ul>
 *   <li><b>{@code [ai]}</b> - AI provider selection</li>
 *   <li><b>{@code [openai]}</b> - API credentials and model settings</li>
 *   <li><b>{@code [behavior]}</b> - Crew behavior settings</li>
 *   <li><b>{@code [voice]}</b> - Voice input/output configuration</li>
 *   <li><b>{@code [hivemind]}</b> - Cloudflare Edge integration</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Get config value
 * String provider = MineWrightConfig.getValidatedProvider();
 * int maxTokens = MineWrightConfig.MAX_TOKENS.get();
 *
 * // Validate configuration
 * boolean isValid = MineWrightConfig.validateAndLog();
 *
 * // Register for reload notifications
 * ConfigManager.getInstance().registerListener(myListener);
 * }</pre>
 *
 * <h2>Configuration Documentation</h2>
 * <p>See {@link ConfigDocumentation} for detailed information about each config option,
 * including default values, valid ranges, and descriptions.</p>
 *
 * @see ConfigDocumentation
 * @see ConfigManager
 * @see ConfigChangeListener
 * @since 1.0.0
 */
public class MineWrightConfig {
    // Valid AI providers
    private static final List<String> VALID_PROVIDERS = Arrays.asList("groq", "openai", "gemini");
    private static final List<String> VALID_VOICE_MODES = Arrays.asList("disabled", "logging", "real");

    // ========================================================================
    // Configuration Specification
    // ========================================================================

    /** The Forge configuration spec */
    public static final ForgeConfigSpec SPEC;

    // ------------------------------------------------------------------------
    // AI Configuration
    // ------------------------------------------------------------------------

    /**
     * AI provider to use for LLM requests.
     * <p><b>Valid values:</b> {@code groq}, {@code openai}, {@code gemini}</p>
     * <p><b>Default:</b> {@code groq}</p>
     * <p><b>Config key:</b> {@code ai.provider}</p>
     *
     * @see ConfigDocumentation#AI
     */
    public static final ForgeConfigSpec.ConfigValue<String> AI_PROVIDER;

    // ------------------------------------------------------------------------
    // OpenAI/Gemini API Configuration
    // ------------------------------------------------------------------------

    /**
     * API key for the chosen provider.
     * <p><b>Required:</b> Yes</p>
     * <p><b>Default:</b> {@code ""} (empty, must be configured)</p>
     * <p><b>Config key:</b> {@code openai.apiKey}</p>
     *
     * @see ConfigDocumentation#OPENAI
     */
    public static final ForgeConfigSpec.ConfigValue<String> OPENAI_API_KEY;

    /**
     * LLM model to use.
     * <p><b>Default:</b> {@code glm-5}</p>
     * <p><b>Config key:</b> {@code openai.model}</p>
     *
     * @see ConfigDocumentation#OPENAI
     */
    public static final ForgeConfigSpec.ConfigValue<String> OPENAI_MODEL;

    /**
     * Maximum tokens per API request.
     * <p><b>Range:</b> 100 to 65536</p>
     * <p><b>Default:</b> 8000</p>
     * <p><b>Config key:</b> {@code openai.maxTokens}</p>
     *
     * @see ConfigDocumentation#OPENAI
     */
    public static final ForgeConfigSpec.IntValue MAX_TOKENS;

    /**
     * Temperature for AI responses (creativity control).
     * <p><b>Range:</b> 0.0 to 2.0</p>
     * <p><b>Default:</b> 0.7</p>
     * <p><b>Config key:</b> {@code openai.temperature}</p>
     *
     * @see ConfigDocumentation#OPENAI
     */
    public static final ForgeConfigSpec.DoubleValue TEMPERATURE;

    // ------------------------------------------------------------------------
    // Behavior Configuration
    // ------------------------------------------------------------------------

    /**
     * Ticks between action checks.
     * <p><b>Range:</b> 1 to 100 (20 ticks = 1 second)</p>
     * <p><b>Default:</b> 20</p>
     * <p><b>Config key:</b> {@code behavior.actionTickDelay}</p>
     *
     * @see ConfigDocumentation#BEHAVIOR
     */
    public static final ForgeConfigSpec.IntValue ACTION_TICK_DELAY;

    /**
     * Allow crew members to respond in chat.
     * <p><b>Default:</b> {@code true}</p>
     * <p><b>Config key:</b> {@code behavior.enableChatResponses}</p>
     *
     * @see ConfigDocumentation#BEHAVIOR
     */
    public static final ForgeConfigSpec.BooleanValue ENABLE_CHAT_RESPONSES;

    /**
     * Maximum number of crew members that can be active simultaneously.
     * <p><b>Range:</b> 1 to 50</p>
     * <p><b>Default:</b> 10</p>
     * <p><b>Config key:</b> {@code behavior.maxActiveCrewMembers}</p>
     *
     * @see ConfigDocumentation#BEHAVIOR
     */
    public static final ForgeConfigSpec.IntValue MAX_ACTIVE_CREW_MEMBERS;

    // ------------------------------------------------------------------------
    // Voice Configuration
    // ------------------------------------------------------------------------

    /**
     * Enable voice input/output features.
     * <p><b>Default:</b> {@code false}</p>
     * <p><b>Config key:</b> {@code voice.enabled}</p>
     *
     * @see ConfigDocumentation#VOICE
     */
    public static final ForgeConfigSpec.BooleanValue VOICE_ENABLED;

    /**
     * Voice system mode.
     * <p><b>Valid values:</b> {@code disabled}, {@code logging}, {@code real}</p>
     * <p><b>Default:</b> {@code logging}</p>
     * <p><b>Config key:</b> {@code voice.mode}</p>
     *
     * @see ConfigDocumentation#VOICE
     */
    public static final ForgeConfigSpec.ConfigValue<String> VOICE_MODE;

    /**
     * Speech-to-text language code.
     * <p><b>Default:</b> {@code en-US}</p>
     * <p><b>Config key:</b> {@code voice.sttLanguage}</p>
     *
     * @see ConfigDocumentation#VOICE
     */
    public static final ForgeConfigSpec.ConfigValue<String> VOICE_STT_LANGUAGE;

    /**
     * Text-to-speech voice name.
     * <p><b>Default:</b> {@code default}</p>
     * <p><b>Config key:</b> {@code voice.ttsVoice}</p>
     *
     * @see ConfigDocumentation#VOICE
     */
    public static final ForgeConfigSpec.ConfigValue<String> VOICE_TTS_VOICE;

    /**
     * TTS volume level.
     * <p><b>Range:</b> 0.0 to 1.0</p>
     * <p><b>Default:</b> 0.8</p>
     * <p><b>Config key:</b> {@code voice.ttsVolume}</p>
     *
     * @see ConfigDocumentation#VOICE
     */
    public static final ForgeConfigSpec.DoubleValue VOICE_TTS_VOLUME;

    /**
     * TTS speech rate.
     * <p><b>Range:</b> 0.5 to 2.0 (1.0 = normal)</p>
     * <p><b>Default:</b> 1.0</p>
     * <p><b>Config key:</b> {@code voice.ttsRate}</p>
     *
     * @see ConfigDocumentation#VOICE
     */
    public static final ForgeConfigSpec.DoubleValue VOICE_TTS_RATE;

    /**
     * TTS pitch adjustment.
     * <p><b>Range:</b> 0.5 to 2.0 (1.0 = normal)</p>
     * <p><b>Default:</b> 1.0</p>
     * <p><b>Config key:</b> {@code voice.ttsPitch}</p>
     *
     * @see ConfigDocumentation#VOICE
     */
    public static final ForgeConfigSpec.DoubleValue VOICE_TTS_PITCH;

    /**
     * STT sensitivity for speech detection.
     * <p><b>Range:</b> 0.0 to 1.0</p>
     * <p><b>Default:</b> 0.5</p>
     * <p><b>Config key:</b> {@code voice.sttSensitivity}</p>
     *
     * @see ConfigDocumentation#VOICE
     */
    public static final ForgeConfigSpec.DoubleValue VOICE_STT_SENSITIVITY;

    /**
     * Require push-to-talk key for voice input.
     * <p><b>Default:</b> {@code true}</p>
     * <p><b>Config key:</b> {@code voice.pushToTalk}</p>
     *
     * @see ConfigDocumentation#VOICE
     */
    public static final ForgeConfigSpec.BooleanValue VOICE_PUSH_TO_TALK;

    /**
     * Auto-stop listening after N seconds of silence.
     * <p><b>Range:</b> 0 to 60 (0 = no timeout)</p>
     * <p><b>Default:</b> 10</p>
     * <p><b>Config key:</b> {@code voice.listeningTimeout}</p>
     *
     * @see ConfigDocumentation#VOICE
     */
    public static final ForgeConfigSpec.IntValue VOICE_LISTENING_TIMEOUT;

    /**
     * Enable verbose logging for voice system operations.
     * <p><b>Default:</b> {@code true}</p>
     * <p><b>Config key:</b> {@code voice.debugLogging}</p>
     *
     * @see ConfigDocumentation#VOICE
     */
    public static final ForgeConfigSpec.BooleanValue VOICE_DEBUG_LOGGING;

    // ------------------------------------------------------------------------
    // Hive Mind (Cloudflare Edge) Configuration
    // ------------------------------------------------------------------------

    /**
     * Enable Hive Mind - distributed AI for tactical reflexes.
     * <p><b>Default:</b> {@code false}</p>
     * <p><b>Config key:</b> {@code hivemind.enabled}</p>
     *
     * @see ConfigDocumentation#HIVEMIND
     */
    public static final ForgeConfigSpec.BooleanValue HIVEMIND_ENABLED;

    /**
     * Cloudflare Worker URL for Hive Mind edge computing.
     * <p><b>Default:</b> {@code https://minecraft-agent-reflex.workers.dev}</p>
     * <p><b>Config key:</b> {@code hivemind.workerUrl}</p>
     *
     * @see ConfigDocumentation#HIVEMIND
     */
    public static final ForgeConfigSpec.ConfigValue<String> HIVEMIND_WORKER_URL;

    /**
     * Connection timeout in milliseconds.
     * <p><b>Range:</b> 500 to 10000</p>
     * <p><b>Default:</b> 2000</p>
     * <p><b>Config key:</b> {@code hivemind.connectTimeoutMs}</p>
     *
     * @see ConfigDocumentation#HIVEMIND
     */
    public static final ForgeConfigSpec.IntValue HIVEMIND_CONNECT_TIMEOUT;

    /**
     * Tactical decision timeout in milliseconds.
     * <p><b>Range:</b> 10 to 500</p>
     * <p><b>Default:</b> 50</p>
     * <p><b>Config key:</b> {@code hivemind.tacticalTimeoutMs}</p>
     *
     * @see ConfigDocumentation#HIVEMIND
     */
    public static final ForgeConfigSpec.IntValue HIVEMIND_TACTICAL_TIMEOUT;

    /**
     * State sync timeout in milliseconds.
     * <p><b>Range:</b> 100 to 5000</p>
     * <p><b>Default:</b> 1000</p>
     * <p><b>Config key:</b> {@code hivemind.syncTimeoutMs}</p>
     *
     * @see ConfigDocumentation#HIVEMIND
     */
    public static final ForgeConfigSpec.IntValue HIVEMIND_SYNC_TIMEOUT;

    /**
     * How often to check for tactical situations (in ticks).
     * <p><b>Range:</b> 5 to 100 (20 ticks = 1 second)</p>
     * <p><b>Default:</b> 20</p>
     * <p><b>Config key:</b> {@code hivemind.tacticalCheckInterval}</p>
     *
     * @see ConfigDocumentation#HIVEMIND
     */
    public static final ForgeConfigSpec.IntValue HIVEMIND_TACTICAL_CHECK_INTERVAL;

    /**
     * How often to sync state with edge (in ticks).
     * <p><b>Range:</b> 20 to 200</p>
     * <p><b>Default:</b> 100</p>
     * <p><b>Config key:</b> {@code hivemind.syncInterval}</p>
     *
     * @see ConfigDocumentation#HIVEMIND
     */
    public static final ForgeConfigSpec.IntValue HIVEMIND_SYNC_INTERVAL;

    /**
     * When edge is unavailable, fall back to local decision-making.
     * <p><b>Default:</b> {@code true}</p>
     * <p><b>Config key:</b> {@code hivemind.fallbackToLocal}</p>
     *
     * @see ConfigDocumentation#HIVEMIND
     */
    public static final ForgeConfigSpec.BooleanValue HIVEMIND_FALLBACK_TO_LOCAL;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("AI API Configuration").push("ai");

        AI_PROVIDER = builder
            .comment("AI provider to use: 'groq' (FASTEST, FREE), 'openai', or 'gemini'")
            .define("provider", "groq");

        builder.pop();

        builder.comment("OpenAI/Gemini API Configuration (same key field used for both)").push("openai");

        OPENAI_API_KEY = builder
            .comment("Your OpenAI API key (required)")
            .define("apiKey", "");

        OPENAI_MODEL = builder
            .comment("LLM model to use (glm-5 for z.ai, gpt-4 for OpenAI)")
            .define("model", "glm-5");

        MAX_TOKENS = builder
            .comment("Maximum tokens per API request")
            .defineInRange("maxTokens", 8000, 100, 65536);

        TEMPERATURE = builder
            .comment("Temperature for AI responses (0.0-2.0, lower is more deterministic)")
            .defineInRange("temperature", 0.7, 0.0, 2.0);

        builder.pop();

        builder.comment("MineWright Behavior Configuration").push("behavior");

        ACTION_TICK_DELAY = builder
            .comment("Ticks between action checks (20 ticks = 1 second)")
            .defineInRange("actionTickDelay", 20, 1, 100);

        ENABLE_CHAT_RESPONSES = builder
            .comment("Allow crew members to respond in chat")
            .define("enableChatResponses", true);

        MAX_ACTIVE_CREW_MEMBERS = builder
            .comment("Maximum number of crew members that can be active simultaneously")
            .defineInRange("maxActiveCrewMembers", 10, 1, 50);

        builder.pop();

        // Voice Configuration
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

        // Hive Mind Configuration (Cloudflare Edge Integration)
        builder.comment("Hive Mind Configuration (Cloudflare Edge for tactical decisions)").push("hivemind");

        HIVEMIND_ENABLED = builder
            .comment("Enable Hive Mind - distributed AI with Cloudflare edge for tactical reflexes",
                     "When enabled, agents get sub-20ms combat/hazard responses from edge workers",
                     "When disabled, all decisions are made locally")
            .define("enabled", false);

        HIVEMIND_WORKER_URL = builder
            .comment("Cloudflare Worker URL (e.g., 'https://minecraft-agent-reflex.your-subdomain.workers.dev')")
            .define("workerUrl", "https://minecraft-agent-reflex.workers.dev");

        HIVEMIND_CONNECT_TIMEOUT = builder
            .comment("Connection timeout in milliseconds (for initial connection)")
            .defineInRange("connectTimeoutMs", 2000, 500, 10000);

        HIVEMIND_TACTICAL_TIMEOUT = builder
            .comment("Tactical decision timeout in milliseconds (target: sub-20ms, max: 100ms)")
            .defineInRange("tacticalTimeoutMs", 50, 10, 500);

        HIVEMIND_SYNC_TIMEOUT = builder
            .comment("State sync timeout in milliseconds (less time-critical)")
            .defineInRange("syncTimeoutMs", 1000, 100, 5000);

        HIVEMIND_TACTICAL_CHECK_INTERVAL = builder
            .comment("How often to check for tactical situations (in ticks, 20 ticks = 1 second)",
                     "Lower = faster reflexes but more API calls",
                     "Recommended: 20 (1 second) for balanced performance")
            .defineInRange("tacticalCheckInterval", 20, 5, 100);

        HIVEMIND_SYNC_INTERVAL = builder
            .comment("How often to sync state with edge (in ticks)",
                     "Higher = less network traffic but potentially stale state",
                     "Recommended: 100 (5 seconds)")
            .defineInRange("syncInterval", 100, 20, 200);

        HIVEMIND_FALLBACK_TO_LOCAL = builder
            .comment("When edge is unavailable, fall back to local decision-making",
                     "If false, agent will wait for edge response (not recommended)")
            .define("fallbackToLocal", true);

        builder.pop();

        SPEC = builder.build();
    }

    /**
     * Validates the configuration and logs warnings for any issues.
     *
     * <p>This method checks for:
     * <ul>
     *   <li>Missing or empty API keys</li>
     *   <li>Invalid AI provider</li>
     *   <li>Invalid voice mode</li>
     *   <li>Out-of-range values (shouldn't happen with Forge's validation)</li>
     * </ul>
     *
     * @return true if all critical configuration is valid, false otherwise
     */
    public static boolean validateAndLog() {
        boolean isValid = true;
        MineWrightMod.LOGGER.info("Validating MineWright configuration...");

        // Validate AI provider
        String provider = AI_PROVIDER.get();
        if (provider == null || provider.trim().isEmpty()) {
            MineWrightMod.LOGGER.warn("AI provider is not set! Defaulting to 'groq'.");
            isValid = false;
        } else if (!VALID_PROVIDERS.contains(provider.toLowerCase())) {
            MineWrightMod.LOGGER.warn("Invalid AI provider '{}'. Valid options: {}. Defaulting to 'groq'.",
                provider, VALID_PROVIDERS);
            isValid = false;
        } else {
            MineWrightMod.LOGGER.info("AI provider: {}", provider);
        }

        // Validate API key
        String apiKey = OPENAI_API_KEY.get();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            MineWrightMod.LOGGER.error("API key is not configured! AI features will not work.");
            MineWrightMod.LOGGER.error("Please set 'apiKey' in [openai] section of config/minewright-common.toml");
            isValid = false;
        } else {
            // Log first few chars to confirm it's set without leaking the full key
            String preview = apiKey.length() > 8
                ? apiKey.substring(0, 4) + "..." + apiKey.substring(apiKey.length() - 4)
                : "****";
            MineWrightMod.LOGGER.info("API key configured: {}", preview);
        }

        // Validate voice mode
        String voiceMode = VOICE_MODE.get();
        if (voiceMode != null && !VALID_VOICE_MODES.contains(voiceMode.toLowerCase())) {
            MineWrightMod.LOGGER.warn("Invalid voice mode '{}'. Valid options: {}. Defaulting to 'disabled'.",
                voiceMode, VALID_VOICE_MODES);
            isValid = false;
        }

        // Log voice status
        if (VOICE_ENABLED.get()) {
            MineWrightMod.LOGGER.info("Voice system: enabled (mode: {})", VOICE_MODE.get());
        } else {
            MineWrightMod.LOGGER.info("Voice system: disabled");
        }

        // Log other important settings
        MineWrightMod.LOGGER.info("Max active crew members: {}", MAX_ACTIVE_CREW_MEMBERS.get());
        MineWrightMod.LOGGER.info("Action tick delay: {} ticks ({} seconds)",
            ACTION_TICK_DELAY.get(), ACTION_TICK_DELAY.get() / 20.0);
        MineWrightMod.LOGGER.info("Chat responses: {}", ENABLE_CHAT_RESPONSES.get() ? "enabled" : "disabled");

        // Log Hive Mind status
        if (HIVEMIND_ENABLED.get()) {
            MineWrightMod.LOGGER.info("Hive Mind: enabled (URL: {})", HIVEMIND_WORKER_URL.get());
        } else {
            MineWrightMod.LOGGER.info("Hive Mind: disabled");
        }

        if (isValid) {
            MineWrightMod.LOGGER.info("MineWright configuration validated successfully.");
        } else {
            MineWrightMod.LOGGER.warn("MineWright configuration has issues. Please check config/minewright-common.toml");
        }

        return isValid;
    }

    /**
     * Validates the configuration and throws an exception for critical errors.
     *
     * <p>Unlike {@link #validateAndLog()}, this method throws a {@link ConfigException}
     * for critical configuration errors.</p>
     *
     * @throws ConfigException if critical configuration errors are found
     * @since 1.5.0
     */
    public static void validateOrThrow() throws ConfigException {
        MineWrightMod.LOGGER.debug("Performing strict configuration validation...");

        // Validate API key (critical)
        String apiKey = OPENAI_API_KEY.get();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw ConfigException.missingKey("apiKey", "openai", true);
        }

        // Validate AI provider
        String provider = AI_PROVIDER.get();
        if (provider == null || provider.trim().isEmpty()) {
            MineWrightMod.LOGGER.warn("AI provider is empty, will use 'groq' as fallback");
        } else if (!VALID_PROVIDERS.contains(provider.toLowerCase())) {
            throw ConfigException.invalidValue("provider", provider,
                String.join(", ", VALID_PROVIDERS), "ai");
        }

        // Validate Hive Mind URL if enabled
        if (HIVEMIND_ENABLED.get()) {
            String workerUrl = HIVEMIND_WORKER_URL.get();
            if (workerUrl != null && !workerUrl.trim().isEmpty()) {
                try {
                    new java.net.URL(workerUrl);
                } catch (java.net.MalformedURLException e) {
                    throw ConfigException.validationFailed("hivemind.workerUrl",
                        "Invalid URL: " + e.getMessage());
                }
            }
        }

        MineWrightMod.LOGGER.debug("Configuration validation passed");
    }

    /**
     * Gets the current AI provider with validation and default fallback.
     *
     * @return The provider name (never null)
     */
    public static String getValidatedProvider() {
        String provider = AI_PROVIDER.get();
        if (provider == null || provider.trim().isEmpty()) {
            MineWrightMod.LOGGER.warn("AI provider not configured, using 'groq' as default");
            return "groq";
        }
        String lowerProvider = provider.toLowerCase();
        if (!VALID_PROVIDERS.contains(lowerProvider)) {
            MineWrightMod.LOGGER.warn("Unknown AI provider '{}', using 'groq' as default", provider);
            return "groq";
        }
        return lowerProvider;
    }

    /**
     * Checks if the API key is configured and non-empty.
     *
     * @return true if API key is present
     */
    public static boolean hasValidApiKey() {
        String key = OPENAI_API_KEY.get();
        return key != null && !key.trim().isEmpty();
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
     * @return Summary string of current configuration
     * @since 1.5.0
     */
    public static String getConfigSummary() {
        return String.format(
            "MineWrightConfig[provider=%s, model=%s, maxTokens=%d, temperature=%.2f, " +
            "actionTickDelay=%d, maxCrew=%d, voice=%s, hivemind=%s]",
            getValidatedProvider(),
            OPENAI_MODEL.get(),
            MAX_TOKENS.get(),
            TEMPERATURE.get(),
            ACTION_TICK_DELAY.get(),
            MAX_ACTIVE_CREW_MEMBERS.get(),
            VOICE_ENABLED.get() ? "enabled(" + VOICE_MODE.get() + ")" : "disabled",
            HIVEMIND_ENABLED.get() ? "enabled" : "disabled"
        );
    }
}

