package com.minewright.config;

import com.minewright.testutil.TestLogger;
import org.slf4j.Logger;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.Arrays;
import java.util.List;

/**
 * Configuration for LLM (Large Language Model) providers and API settings.
 *
 * <h2>Configuration Section</h2>
 * <p><b>{@code [ai]}</b> - AI provider selection</p>
 * <p><b>{@code [openai]}</b> - API credentials and model settings</p>
 *
 * <h2>Supported Providers</h2>
 * <ul>
 *   <li><b>openai</b> - z.ai GLM models (RECOMMENDED)</li>
 *   <li><b>groq</b> - Groq API</li>
 *   <li><b>gemini</b> - Google Gemini API</li>
 * </ul>
 *
 * @since 3.0.0
 */
public class LLMConfig {
    private static final Logger LOGGER = TestLogger.getLogger(LLMConfig.class);
    private static final List<String> VALID_PROVIDERS = Arrays.asList("openai", "groq", "gemini");

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
     */
    public static final ForgeConfigSpec.ConfigValue<String> OPENAI_API_KEY;

    /**
     * LLM model to use.
     * <p><b>Default:</b> {@code glm-5}</p>
     * <p><b>Config key:</b> {@code openai.model}</p>
     */
    public static final ForgeConfigSpec.ConfigValue<String> OPENAI_MODEL;

    /**
     * Maximum tokens per API request.
     * <p><b>Range:</b> 100 to 65536</p>
     * <p><b>Default:</b> 8000</p>
     * <p><b>Config key:</b> {@code openai.maxTokens}</p>
     */
    public static final ForgeConfigSpec.IntValue MAX_TOKENS;

    /**
     * Temperature for AI responses (creativity control).
     * <p><b>Range:</b> 0.0 to 2.0</p>
     * <p><b>Default:</b> 0.7</p>
     * <p><b>Config key:</b> {@code openai.temperature}</p>
     */
    public static final ForgeConfigSpec.DoubleValue TEMPERATURE;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("AI API Configuration").push("ai");

        AI_PROVIDER = builder
            .comment("AI provider to use: 'openai' (z.ai GLM-5, RECOMMENDED), 'groq', or 'gemini'")
            .define("provider", "openai");

        builder.pop();

        builder.comment("z.ai/OpenAI API Configuration").push("openai");

        OPENAI_API_KEY = builder
            .comment("Your z.ai API key (required - get from console.z.ai)")
            .define("apiKey", "");

        OPENAI_MODEL = builder
            .comment("LLM model: 'glm-5' (recommended), 'glm-4-flash' (faster), or 'gpt-4' (OpenAI)")
            .define("model", "glm-5");

        MAX_TOKENS = builder
            .comment("Maximum tokens per API request")
            .defineInRange("maxTokens", 8000, 100, 65536);

        TEMPERATURE = builder
            .comment("Temperature for AI responses (0.0-2.0, lower is more deterministic)")
            .defineInRange("temperature", 0.7, 0.0, 2.0);

        builder.pop();

        SPEC = builder.build();
    }

    /**
     * Validates the LLM configuration.
     *
     * @return true if configuration is valid
     */
    public static boolean validate() {
        boolean isValid = true;
        LOGGER.info("Validating LLM configuration...");

        // Validate AI provider
        String provider = AI_PROVIDER.get();
        if (provider == null || provider.trim().isEmpty()) {
            LOGGER.warn("AI provider is not set! Defaulting to 'openai' (z.ai).");
            isValid = false;
        } else if (!VALID_PROVIDERS.contains(provider.toLowerCase())) {
            LOGGER.warn("Invalid AI provider '{}'. Valid options: {}. Defaulting to 'openai' (z.ai).",
                provider, VALID_PROVIDERS);
            isValid = false;
        } else {
            LOGGER.info("AI provider: {}", provider);
        }

        // Validate API key
        String apiKey = OPENAI_API_KEY.get();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            LOGGER.error("API key is not configured! AI features will not work.");
            LOGGER.error("Please set 'apiKey' in [openai] section of config/minewright-common.toml");
            isValid = false;
        } else {
            // Log first few chars to confirm it's set without leaking the full key
            String preview = apiKey.length() > 8
                ? apiKey.substring(0, 4) + "..." + apiKey.substring(apiKey.length() - 4)
                : "****";
            LOGGER.info("API key configured: {}", preview);
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
            LOGGER.warn("AI provider not configured, using 'openai' (z.ai) as default");
            return "openai";
        }
        String lowerProvider = provider.toLowerCase();
        if (!VALID_PROVIDERS.contains(lowerProvider)) {
            LOGGER.warn("Unknown AI provider '{}', using 'openai' (z.ai) as default", provider);
            return "openai";
        }
        return lowerProvider;
    }

    /**
     * Checks if the API key is configured and non-empty.
     *
     * @return true if API key is present
     */
    public static boolean hasValidApiKey() {
        String key = getResolvedApiKey();
        return key != null && !key.trim().isEmpty();
    }

    /**
     * Gets the API key with environment variable resolution.
     *
     * <p>If the config value is in format {@code ${ENV_VAR_NAME}}, it will be
     * resolved from the environment. Otherwise returns the value as-is.</p>
     *
     * @return The resolved API key, or empty string if not set
     */
    public static String getResolvedApiKey() {
        String key = OPENAI_API_KEY.get();
        return resolveEnvVar(key);
    }

    /**
     * Resolves a configuration value that may contain an environment variable reference.
     *
     * <p>Supports the format: {@code ${ENV_VAR_NAME}}</p>
     *
     * @param value The config value, possibly containing ${ENV_VAR} syntax
     * @return The resolved value, or the original if not an env var reference
     */
    public static String resolveEnvVar(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        // Check for ${ENV_VAR} syntax
        if (value.startsWith("${") && value.endsWith("}")) {
            String envVarName = value.substring(2, value.length() - 1);
            String envValue = System.getenv(envVarName);
            if (envValue != null && !envValue.isEmpty()) {
                return envValue;
            }
            LOGGER.warn("Environment variable '{}' is not set or empty", envVarName);
            return "";
        }

        return value;
    }

    /**
     * Gets a configuration summary for logging/debugging.
     *
     * @return Summary string of current LLM configuration
     */
    public static String getConfigSummary() {
        return String.format(
            "LLMConfig[provider=%s, model=%s, maxTokens=%d, temperature=%.2f]",
            getValidatedProvider(),
            OPENAI_MODEL.get(),
            MAX_TOKENS.get(),
            TEMPERATURE.get()
        );
    }
}
