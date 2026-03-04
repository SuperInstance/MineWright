package com.minewright.config;

import com.minewright.testutil.TestLogger;
import org.slf4j.Logger;
import com.minewright.exception.ConfigException;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Main configuration class for MineWright mod.
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
 * <h2>Architecture</h2>
 * <p>This class delegates to specialized configuration classes for better organization:</p>
 * <ul>
 *   <li>{@link LLMConfig} - AI provider and API settings</li>
 *   <li>{@link VoiceConfig} - Voice input/output settings</li>
 *   <li>{@link BehaviorConfig} - Crew behavior settings</li>
 *   <li>{@link HiveMindConfig} - Cloudflare Edge integration</li>
 *   <li>{@link SkillLibraryConfig} - Skill learning system</li>
 *   <li>{@link CascadeRouterConfig} - Intelligent LLM selection</li>
 *   <li>{@link UtilityAIConfig} - Decision-making weights</li>
 *   <li>{@link MultiAgentConfig} - Coordination features</li>
 *   <li>{@link PathfindingConfig} - Navigation algorithms</li>
 *   <li>{@link PerformanceConfig} - Performance tuning</li>
 *   <li>{@link SemanticCacheConfig} - LLM response caching</li>
 *   <li>{@link HumanizationConfig} - Natural agent behavior</li>
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
    private static final Logger LOGGER = TestLogger.getLogger(MineWrightConfig.class);

    // ========================================================================
    // Backward Compatibility - Delegate to specialized config classes
    // ========================================================================

    // ------------------------------------------------------------------------
    // AI Configuration (delegates to LLMConfig)
    // ------------------------------------------------------------------------

    /** @see LLMConfig#AI_PROVIDER */
    public static final ForgeConfigSpec.ConfigValue<String> AI_PROVIDER = LLMConfig.AI_PROVIDER;

    /** @see LLMConfig#OPENAI_API_KEY */
    public static final ForgeConfigSpec.ConfigValue<String> OPENAI_API_KEY = LLMConfig.OPENAI_API_KEY;

    /** @see LLMConfig#OPENAI_MODEL */
    public static final ForgeConfigSpec.ConfigValue<String> OPENAI_MODEL = LLMConfig.OPENAI_MODEL;

    /** @see LLMConfig#MAX_TOKENS */
    public static final ForgeConfigSpec.IntValue MAX_TOKENS = LLMConfig.MAX_TOKENS;

    /** @see LLMConfig#TEMPERATURE */
    public static final ForgeConfigSpec.DoubleValue TEMPERATURE = LLMConfig.TEMPERATURE;

    // ------------------------------------------------------------------------
    // Behavior Configuration (delegates to BehaviorConfig)
    // ------------------------------------------------------------------------

    /** @see BehaviorConfig#ACTION_TICK_DELAY */
    public static final ForgeConfigSpec.IntValue ACTION_TICK_DELAY = BehaviorConfig.ACTION_TICK_DELAY;

    /** @see BehaviorConfig#ENABLE_CHAT_RESPONSES */
    public static final ForgeConfigSpec.BooleanValue ENABLE_CHAT_RESPONSES = BehaviorConfig.ENABLE_CHAT_RESPONSES;

    /** @see BehaviorConfig#MAX_ACTIVE_CREW_MEMBERS */
    public static final ForgeConfigSpec.IntValue MAX_ACTIVE_CREW_MEMBERS = BehaviorConfig.MAX_ACTIVE_CREW_MEMBERS;

    // ------------------------------------------------------------------------
    // Voice Configuration (delegates to VoiceConfig)
    // ------------------------------------------------------------------------

    /** @see VoiceConfig#VOICE_ENABLED */
    public static final ForgeConfigSpec.BooleanValue VOICE_ENABLED = VoiceConfig.VOICE_ENABLED;

    /** @see VoiceConfig#VOICE_MODE */
    public static final ForgeConfigSpec.ConfigValue<String> VOICE_MODE = VoiceConfig.VOICE_MODE;

    /** @see VoiceConfig#VOICE_STT_LANGUAGE */
    public static final ForgeConfigSpec.ConfigValue<String> VOICE_STT_LANGUAGE = VoiceConfig.VOICE_STT_LANGUAGE;

    /** @see VoiceConfig#VOICE_TTS_VOICE */
    public static final ForgeConfigSpec.ConfigValue<String> VOICE_TTS_VOICE = VoiceConfig.VOICE_TTS_VOICE;

    /** @see VoiceConfig#VOICE_TTS_VOLUME */
    public static final ForgeConfigSpec.DoubleValue VOICE_TTS_VOLUME = VoiceConfig.VOICE_TTS_VOLUME;

    /** @see VoiceConfig#VOICE_TTS_RATE */
    public static final ForgeConfigSpec.DoubleValue VOICE_TTS_RATE = VoiceConfig.VOICE_TTS_RATE;

    /** @see VoiceConfig#VOICE_TTS_PITCH */
    public static final ForgeConfigSpec.DoubleValue VOICE_TTS_PITCH = VoiceConfig.VOICE_TTS_PITCH;

    /** @see VoiceConfig#VOICE_STT_SENSITIVITY */
    public static final ForgeConfigSpec.DoubleValue VOICE_STT_SENSITIVITY = VoiceConfig.VOICE_STT_SENSITIVITY;

    /** @see VoiceConfig#VOICE_PUSH_TO_TALK */
    public static final ForgeConfigSpec.BooleanValue VOICE_PUSH_TO_TALK = VoiceConfig.VOICE_PUSH_TO_TALK;

    /** @see VoiceConfig#VOICE_LISTENING_TIMEOUT */
    public static final ForgeConfigSpec.IntValue VOICE_LISTENING_TIMEOUT = VoiceConfig.VOICE_LISTENING_TIMEOUT;

    /** @see VoiceConfig#VOICE_DEBUG_LOGGING */
    public static final ForgeConfigSpec.BooleanValue VOICE_DEBUG_LOGGING = VoiceConfig.VOICE_DEBUG_LOGGING;

    // ------------------------------------------------------------------------
    // Hive Mind Configuration (delegates to HiveMindConfig)
    // ------------------------------------------------------------------------

    /** @see HiveMindConfig#HIVEMIND_ENABLED */
    public static final ForgeConfigSpec.BooleanValue HIVEMIND_ENABLED = HiveMindConfig.HIVEMIND_ENABLED;

    /** @see HiveMindConfig#HIVEMIND_WORKER_URL */
    public static final ForgeConfigSpec.ConfigValue<String> HIVEMIND_WORKER_URL = HiveMindConfig.HIVEMIND_WORKER_URL;

    /** @see HiveMindConfig#HIVEMIND_CONNECT_TIMEOUT */
    public static final ForgeConfigSpec.IntValue HIVEMIND_CONNECT_TIMEOUT = HiveMindConfig.HIVEMIND_CONNECT_TIMEOUT;

    /** @see HiveMindConfig#HIVEMIND_TACTICAL_TIMEOUT */
    public static final ForgeConfigSpec.IntValue HIVEMIND_TACTICAL_TIMEOUT = HiveMindConfig.HIVEMIND_TACTICAL_TIMEOUT;

    /** @see HiveMindConfig#HIVEMIND_SYNC_TIMEOUT */
    public static final ForgeConfigSpec.IntValue HIVEMIND_SYNC_TIMEOUT = HiveMindConfig.HIVEMIND_SYNC_TIMEOUT;

    /** @see HiveMindConfig#HIVEMIND_TACTICAL_CHECK_INTERVAL */
    public static final ForgeConfigSpec.IntValue HIVEMIND_TACTICAL_CHECK_INTERVAL = HiveMindConfig.HIVEMIND_TACTICAL_CHECK_INTERVAL;

    /** @see HiveMindConfig#HIVEMIND_SYNC_INTERVAL */
    public static final ForgeConfigSpec.IntValue HIVEMIND_SYNC_INTERVAL = HiveMindConfig.HIVEMIND_SYNC_INTERVAL;

    /** @see HiveMindConfig#HIVEMIND_FALLBACK_TO_LOCAL */
    public static final ForgeConfigSpec.BooleanValue HIVEMIND_FALLBACK_TO_LOCAL = HiveMindConfig.HIVEMIND_FALLBACK_TO_LOCAL;

    // ------------------------------------------------------------------------
    // Skill Library Configuration (delegates to SkillLibraryConfig)
    // ------------------------------------------------------------------------

    /** @see SkillLibraryConfig#SKILL_LIBRARY_ENABLED */
    public static final ForgeConfigSpec.BooleanValue SKILL_LIBRARY_ENABLED = SkillLibraryConfig.SKILL_LIBRARY_ENABLED;

    /** @see SkillLibraryConfig#MAX_SKILLS_STORED */
    public static final ForgeConfigSpec.IntValue MAX_SKILLS_STORED = SkillLibraryConfig.MAX_SKILLS_STORED;

    /** @see SkillLibraryConfig#SKILL_SUCCESS_THRESHOLD */
    public static final ForgeConfigSpec.DoubleValue SKILL_SUCCESS_THRESHOLD = SkillLibraryConfig.SKILL_SUCCESS_THRESHOLD;

    // ------------------------------------------------------------------------
    // Cascade Router Configuration (delegates to CascadeRouterConfig)
    // ------------------------------------------------------------------------

    /** @see CascadeRouterConfig#CASCADE_ROUTER_ENABLED */
    public static final ForgeConfigSpec.BooleanValue CASCADE_ROUTER_ENABLED = CascadeRouterConfig.CASCADE_ROUTER_ENABLED;

    /** @see CascadeRouterConfig#SEMANTIC_SIMILARITY_THRESHOLD */
    public static final ForgeConfigSpec.DoubleValue SEMANTIC_SIMILARITY_THRESHOLD = CascadeRouterConfig.SEMANTIC_SIMILARITY_THRESHOLD;

    /** @see CascadeRouterConfig#USE_LOCAL_LLM */
    public static final ForgeConfigSpec.BooleanValue USE_LOCAL_LLM = CascadeRouterConfig.USE_LOCAL_LLM;

    // ------------------------------------------------------------------------
    // Utility AI Configuration (delegates to UtilityAIConfig)
    // ------------------------------------------------------------------------

    /** @see UtilityAIConfig#UTILITY_AI_ENABLED */
    public static final ForgeConfigSpec.BooleanValue UTILITY_AI_ENABLED = UtilityAIConfig.UTILITY_AI_ENABLED;

    /** @see UtilityAIConfig#URGENCY_WEIGHT */
    public static final ForgeConfigSpec.DoubleValue URGENCY_WEIGHT = UtilityAIConfig.URGENCY_WEIGHT;

    /** @see UtilityAIConfig#PROXIMITY_WEIGHT */
    public static final ForgeConfigSpec.DoubleValue PROXIMITY_WEIGHT = UtilityAIConfig.PROXIMITY_WEIGHT;

    /** @see UtilityAIConfig#SAFETY_WEIGHT */
    public static final ForgeConfigSpec.DoubleValue SAFETY_WEIGHT = UtilityAIConfig.SAFETY_WEIGHT;

    // ------------------------------------------------------------------------
    // Multi-Agent Configuration (delegates to MultiAgentConfig)
    // ------------------------------------------------------------------------

    /** @see MultiAgentConfig#MULTI_AGENT_ENABLED */
    public static final ForgeConfigSpec.BooleanValue MULTI_AGENT_ENABLED = MultiAgentConfig.MULTI_AGENT_ENABLED;

    /** @see MultiAgentConfig#MAX_BID_WAIT_MS */
    public static final ForgeConfigSpec.IntValue MAX_BID_WAIT_MS = MultiAgentConfig.MAX_BID_WAIT_MS;

    /** @see MultiAgentConfig#BLACKBOARD_TTL_SECONDS */
    public static final ForgeConfigSpec.IntValue BLACKBOARD_TTL_SECONDS = MultiAgentConfig.BLACKBOARD_TTL_SECONDS;

    // ------------------------------------------------------------------------
    // Pathfinding Configuration (delegates to PathfindingConfig)
    // ------------------------------------------------------------------------

    /** @see PathfindingConfig#ENHANCED_PATHFINDING */
    public static final ForgeConfigSpec.BooleanValue ENHANCED_PATHFINDING = PathfindingConfig.ENHANCED_PATHFINDING;

    /** @see PathfindingConfig#MAX_PATH_SEARCH_NODES */
    public static final ForgeConfigSpec.IntValue MAX_PATH_SEARCH_NODES = PathfindingConfig.MAX_PATH_SEARCH_NODES;

    /** @see PathfindingConfig#PATHFINDING_MAX_NODES */
    public static final ForgeConfigSpec.IntValue PATHFINDING_MAX_NODES = PathfindingConfig.PATHFINDING_MAX_NODES;

    /** @see PathfindingConfig#PATHFINDING_CACHE_ENABLED */
    public static final ForgeConfigSpec.BooleanValue PATHFINDING_CACHE_ENABLED = PathfindingConfig.PATHFINDING_CACHE_ENABLED;

    /** @see PathfindingConfig#PATHFINDING_CACHE_MAX_SIZE */
    public static final ForgeConfigSpec.IntValue PATHFINDING_CACHE_MAX_SIZE = PathfindingConfig.PATHFINDING_CACHE_MAX_SIZE;

    /** @see PathfindingConfig#PATHFINDING_CACHE_TTL_MINUTES */
    public static final ForgeConfigSpec.IntValue PATHFINDING_CACHE_TTL_MINUTES = PathfindingConfig.PATHFINDING_CACHE_TTL_MINUTES;

    // ------------------------------------------------------------------------
    // Performance Configuration (delegates to PerformanceConfig and PathfindingConfig)
    // ------------------------------------------------------------------------

    /** @see PerformanceConfig#AI_TICK_BUDGET_MS */
    public static final ForgeConfigSpec.IntValue AI_TICK_BUDGET_MS = PerformanceConfig.AI_TICK_BUDGET_MS;

    /** @see PerformanceConfig#BUDGET_WARNING_THRESHOLD */
    public static final ForgeConfigSpec.IntValue BUDGET_WARNING_THRESHOLD = PerformanceConfig.BUDGET_WARNING_THRESHOLD;

    /** @see PerformanceConfig#STRICT_BUDGET_ENFORCEMENT */
    public static final ForgeConfigSpec.BooleanValue STRICT_BUDGET_ENFORCEMENT = PerformanceConfig.STRICT_BUDGET_ENFORCEMENT;

    /** @see PathfindingConfig#PATHFINDING_TIMEOUT_MS */
    public static final ForgeConfigSpec.IntValue PATHFINDING_TIMEOUT_MS = PathfindingConfig.PATHFINDING_TIMEOUT_MS;

    // ------------------------------------------------------------------------
    // Semantic Cache Configuration (delegates to SemanticCacheConfig)
    // ------------------------------------------------------------------------

    /** @see SemanticCacheConfig#SEMANTIC_CACHE_ENABLED */
    public static final ForgeConfigSpec.BooleanValue SEMANTIC_CACHE_ENABLED = SemanticCacheConfig.SEMANTIC_CACHE_ENABLED;

    /** @see SemanticCacheConfig#SEMANTIC_CACHE_SIMILARITY_THRESHOLD */
    public static final ForgeConfigSpec.DoubleValue SEMANTIC_CACHE_SIMILARITY_THRESHOLD = SemanticCacheConfig.SEMANTIC_CACHE_SIMILARITY_THRESHOLD;

    /** @see SemanticCacheConfig#SEMANTIC_CACHE_MAX_SIZE */
    public static final ForgeConfigSpec.IntValue SEMANTIC_CACHE_MAX_SIZE = SemanticCacheConfig.SEMANTIC_CACHE_MAX_SIZE;

    /** @see SemanticCacheConfig#SEMANTIC_CACHE_TTL_MINUTES */
    public static final ForgeConfigSpec.IntValue SEMANTIC_CACHE_TTL_MINUTES = SemanticCacheConfig.SEMANTIC_CACHE_TTL_MINUTES;

    /** @see SemanticCacheConfig#SEMANTIC_CACHE_EMBEDDING_METHOD */
    public static final ForgeConfigSpec.ConfigValue<String> SEMANTIC_CACHE_EMBEDDING_METHOD = SemanticCacheConfig.SEMANTIC_CACHE_EMBEDDING_METHOD;

    // ------------------------------------------------------------------------
    // Humanization Configuration (delegates to HumanizationConfig)
    // ------------------------------------------------------------------------

    /** @see HumanizationConfig#HUMANIZATION_ENABLED */
    public static final ForgeConfigSpec.BooleanValue HUMANIZATION_ENABLED = HumanizationConfig.HUMANIZATION_ENABLED;

    /** @see HumanizationConfig#TIMING_VARIANCE */
    public static final ForgeConfigSpec.DoubleValue TIMING_VARIANCE = HumanizationConfig.TIMING_VARIANCE;

    /** @see HumanizationConfig#MIN_ACTION_DELAY_TICKS */
    public static final ForgeConfigSpec.IntValue MIN_ACTION_DELAY_TICKS = HumanizationConfig.MIN_ACTION_DELAY_TICKS;

    /** @see HumanizationConfig#MAX_ACTION_DELAY_TICKS */
    public static final ForgeConfigSpec.IntValue MAX_ACTION_DELAY_TICKS = HumanizationConfig.MAX_ACTION_DELAY_TICKS;

    /** @see HumanizationConfig#SPEED_VARIANCE */
    public static final ForgeConfigSpec.DoubleValue SPEED_VARIANCE = HumanizationConfig.SPEED_VARIANCE;

    /** @see HumanizationConfig#MICRO_MOVEMENT_CHANCE */
    public static final ForgeConfigSpec.DoubleValue MICRO_MOVEMENT_CHANCE = HumanizationConfig.MICRO_MOVEMENT_CHANCE;

    /** @see HumanizationConfig#SMOOTH_LOOK */
    public static final ForgeConfigSpec.BooleanValue SMOOTH_LOOK = HumanizationConfig.SMOOTH_LOOK;

    /** @see HumanizationConfig#MISTAKE_RATE */
    public static final ForgeConfigSpec.DoubleValue MISTAKE_RATE = HumanizationConfig.MISTAKE_RATE;

    /** @see HumanizationConfig#REACTION_TIME_MIN_MS */
    public static final ForgeConfigSpec.IntValue REACTION_TIME_MIN_MS = HumanizationConfig.REACTION_TIME_MIN_MS;

    /** @see HumanizationConfig#REACTION_TIME_MAX_MS */
    public static final ForgeConfigSpec.IntValue REACTION_TIME_MAX_MS = HumanizationConfig.REACTION_TIME_MAX_MS;

    /** @see HumanizationConfig#IDLE_ACTION_CHANCE */
    public static final ForgeConfigSpec.DoubleValue IDLE_ACTION_CHANCE = HumanizationConfig.IDLE_ACTION_CHANCE;

    /** @see HumanizationConfig#PERSONALITY_AFFECTS_IDLE */
    public static final ForgeConfigSpec.BooleanValue PERSONALITY_AFFECTS_IDLE = HumanizationConfig.PERSONALITY_AFFECTS_IDLE;

    /** @see HumanizationConfig#SESSION_MODELING_ENABLED */
    public static final ForgeConfigSpec.BooleanValue SESSION_MODELING_ENABLED = HumanizationConfig.SESSION_MODELING_ENABLED;

    /** @see HumanizationConfig#WARMUP_DURATION_MINUTES */
    public static final ForgeConfigSpec.IntValue WARMUP_DURATION_MINUTES = HumanizationConfig.WARMUP_DURATION_MINUTES;

    /** @see HumanizationConfig#FATIGUE_START_MINUTES */
    public static final ForgeConfigSpec.IntValue FATIGUE_START_MINUTES = HumanizationConfig.FATIGUE_START_MINUTES;

    /** @see HumanizationConfig#BREAK_INTERVAL_MINUTES */
    public static final ForgeConfigSpec.IntValue BREAK_INTERVAL_MINUTES = HumanizationConfig.BREAK_INTERVAL_MINUTES;

    /** @see HumanizationConfig#BREAK_DURATION_MINUTES */
    public static final ForgeConfigSpec.IntValue BREAK_DURATION_MINUTES = HumanizationConfig.BREAK_DURATION_MINUTES;

    // ========================================================================
    // Validation and Utility Methods
    // ========================================================================

    /**
     * Validates the configuration and logs warnings for any issues.
     *
     * <p>This method checks all configuration sections and logs any issues found.</p>
     *
     * @return true if all critical configuration is valid, false otherwise
     */
    public static boolean validateAndLog() {
        boolean isValid = true;
        LOGGER.info("Validating MineWright configuration...");

        isValid &= LLMConfig.validate();
        isValid &= VoiceConfig.validate();
        isValid &= BehaviorConfig.validate();

        try {
            HiveMindConfig.validate();
        } catch (ConfigException e) {
            LOGGER.error("Hive Mind configuration error: {}", e.getMessage());
            isValid = false;
        }

        isValid &= SkillLibraryConfig.validate();

        try {
            CascadeRouterConfig.validate();
        } catch (ConfigException e) {
            LOGGER.error("Cascade Router configuration error: {}", e.getMessage());
            isValid = false;
        }

        try {
            UtilityAIConfig.validate();
        } catch (ConfigException e) {
            LOGGER.error("Utility AI configuration error: {}", e.getMessage());
            isValid = false;
        }

        isValid &= MultiAgentConfig.validate();
        isValid &= PathfindingConfig.validate();
        isValid &= PerformanceConfig.validate();
        isValid &= SemanticCacheConfig.validate();
        isValid &= HumanizationConfig.validate();

        if (isValid) {
            LOGGER.info("MineWright configuration validated successfully.");
        } else {
            LOGGER.warn("MineWright configuration has issues. Please check config/minewright-common.toml");
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
        LOGGER.debug("Performing strict configuration validation...");

        // Validate LLM (critical)
        if (!LLMConfig.hasValidApiKey()) {
            throw ConfigException.missingKey("apiKey", "openai", true);
        }

        String provider = LLMConfig.AI_PROVIDER.get();
        if (provider == null || provider.trim().isEmpty()) {
            LOGGER.warn("AI provider is empty, will use 'openai' (z.ai) as fallback");
        } else if (!LLMConfig.getValidatedProvider().equals(provider.toLowerCase())) {
            throw ConfigException.invalidValue("provider", provider,
                "openai, groq, gemini", "ai");
        }

        // Validate Hive Mind URL if enabled
        HiveMindConfig.validate();

        // Validate thresholds are 0-1
        SkillLibraryConfig.validate();
        CascadeRouterConfig.validate();
        UtilityAIConfig.validate();

        LOGGER.debug("Configuration validation passed");
    }

    /**
     * Gets the current AI provider with validation and default fallback.
     *
     * @return The provider name (never null)
     * @see LLMConfig#getValidatedProvider()
     */
    public static String getValidatedProvider() {
        return LLMConfig.getValidatedProvider();
    }

    /**
     * Checks if the API key is configured and non-empty.
     *
     * @return true if API key is present
     * @see LLMConfig#hasValidApiKey()
     */
    public static boolean hasValidApiKey() {
        return LLMConfig.hasValidApiKey();
    }

    /**
     * Gets the API key with environment variable resolution.
     *
     * @return The resolved API key, or empty string if not set
     * @see LLMConfig#getResolvedApiKey()
     */
    public static String getResolvedApiKey() {
        return LLMConfig.getResolvedApiKey();
    }

    /**
     * Resolves a configuration value that may contain an environment variable reference.
     *
     * @param value The config value, possibly containing ${ENV_VAR} syntax
     * @return The resolved value, or the original if not an env var reference
     * @see LLMConfig#resolveEnvVar(String)
     */
    public static String resolveEnvVar(String value) {
        return LLMConfig.resolveEnvVar(value);
    }

    /**
     * Checks if voice is properly configured and enabled.
     *
     * @return true if voice is enabled
     * @see VoiceConfig#isVoiceEnabled()
     */
    public static boolean isVoiceEnabled() {
        return VoiceConfig.isVoiceEnabled();
    }

    /**
     * Gets the validated voice mode.
     *
     * @return The voice mode (never null)
     * @see VoiceConfig#getValidatedVoiceMode()
     */
    public static String getValidatedVoiceMode() {
        return VoiceConfig.getValidatedVoiceMode();
    }

    /**
     * Gets a configuration summary for logging/debugging.
     *
     * @return Summary string of current configuration
     * @since 1.5.0
     */
    public static String getConfigSummary() {
        return String.format(
            "MineWrightConfig[%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s]",
            LLMConfig.getConfigSummary(),
            BehaviorConfig.getConfigSummary(),
            VoiceConfig.getConfigSummary(),
            HiveMindConfig.getConfigSummary(),
            SkillLibraryConfig.getConfigSummary(),
            CascadeRouterConfig.getConfigSummary(),
            UtilityAIConfig.getConfigSummary(),
            MultiAgentConfig.getConfigSummary(),
            PathfindingConfig.getConfigSummary(),
            PerformanceConfig.getConfigSummary(),
            SemanticCacheConfig.getConfigSummary(),
            HumanizationConfig.getConfigSummary()
        );
    }
}
