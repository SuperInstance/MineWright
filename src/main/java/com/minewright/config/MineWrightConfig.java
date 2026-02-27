package com.minewright.config;

import com.minewright.MineWrightMod;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.Arrays;
import java.util.List;

/**
 * Configuration for MineWright mod.
 *
 * <p>Config file location: <code>config/minewright-common.toml</code></p>
 *
 * <p><b>Platform Notes:</b></p>
 * <ul>
 *   <li><b>Windows:</b> <code>.minecraft/config/minewright-common.toml</code></li>
 *   <li><b>Linux/Mac:</b> <code>~/.minecraft/config/minewright-common.toml</code></li>
 * </ul>
 *
 * <p><b>Config Reload:</b> Edit the config file and use <code>/reload</code> to reload without restart.</p>
 */
public class MineWrightConfig {
    // Valid AI providers
    private static final List<String> VALID_PROVIDERS = Arrays.asList("groq", "openai", "gemini");
    private static final List<String> VALID_VOICE_MODES = Arrays.asList("disabled", "logging", "real");

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.ConfigValue<String> AI_PROVIDER;
    public static final ForgeConfigSpec.ConfigValue<String> OPENAI_API_KEY;
    public static final ForgeConfigSpec.ConfigValue<String> OPENAI_MODEL;
    public static final ForgeConfigSpec.IntValue MAX_TOKENS;
    public static final ForgeConfigSpec.DoubleValue TEMPERATURE;
    public static final ForgeConfigSpec.IntValue ACTION_TICK_DELAY;
    public static final ForgeConfigSpec.BooleanValue ENABLE_CHAT_RESPONSES;
    public static final ForgeConfigSpec.IntValue MAX_ACTIVE_CREW_MEMBERS;

    // Voice configuration
    public static final ForgeConfigSpec.BooleanValue VOICE_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<String> VOICE_MODE;
    public static final ForgeConfigSpec.ConfigValue<String> VOICE_STT_LANGUAGE;
    public static final ForgeConfigSpec.ConfigValue<String> VOICE_TTS_VOICE;
    public static final ForgeConfigSpec.DoubleValue VOICE_TTS_VOLUME;
    public static final ForgeConfigSpec.DoubleValue VOICE_TTS_RATE;
    public static final ForgeConfigSpec.DoubleValue VOICE_TTS_PITCH;
    public static final ForgeConfigSpec.DoubleValue VOICE_STT_SENSITIVITY;
    public static final ForgeConfigSpec.BooleanValue VOICE_PUSH_TO_TALK;
    public static final ForgeConfigSpec.IntValue VOICE_LISTENING_TIMEOUT;
    public static final ForgeConfigSpec.BooleanValue VOICE_DEBUG_LOGGING;

    // Hive Mind (Cloudflare Edge) Configuration
    public static final ForgeConfigSpec.BooleanValue HIVEMIND_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<String> HIVEMIND_WORKER_URL;
    public static final ForgeConfigSpec.IntValue HIVEMIND_CONNECT_TIMEOUT;
    public static final ForgeConfigSpec.IntValue HIVEMIND_TACTICAL_TIMEOUT;
    public static final ForgeConfigSpec.IntValue HIVEMIND_SYNC_TIMEOUT;
    public static final ForgeConfigSpec.IntValue HIVEMIND_TACTICAL_CHECK_INTERVAL;
    public static final ForgeConfigSpec.IntValue HIVEMIND_SYNC_INTERVAL;
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

        if (isValid) {
            MineWrightMod.LOGGER.info("MineWright configuration validated successfully.");
        } else {
            MineWrightMod.LOGGER.warn("MineWright configuration has issues. Please check config/minewright-common.toml");
        }

        return isValid;
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
}

